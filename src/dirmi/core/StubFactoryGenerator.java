/*
 *  Copyright 2006 Brian S O'Neill
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dirmi.core;

import java.io.IOException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.rmi.NoSuchObjectException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.List;
import java.util.Map;

import org.cojen.classfile.ClassFile;
import org.cojen.classfile.CodeBuilder;
import org.cojen.classfile.Label;
import org.cojen.classfile.LocalVariable;
import org.cojen.classfile.MethodInfo;
import org.cojen.classfile.Modifiers;
import org.cojen.classfile.TypeDesc;

import org.cojen.util.ClassInjector;
import org.cojen.util.KeyFactory;
import org.cojen.util.SoftValuedHashMap;

import dirmi.UnimplementedMethodException;

import dirmi.core.Identifier;

import dirmi.info.RemoteInfo;
import dirmi.info.RemoteIntrospector;
import dirmi.info.RemoteMethod;
import dirmi.info.RemoteParameter;

import dirmi.io.RemoteConnection;
import dirmi.io.RemoteInput;
import dirmi.io.RemoteOutput;

/**
 * 
 *
 * @author Brian S O'Neill
 */
public class StubFactoryGenerator<R extends Remote> {
    private static final String STUB_SUPPORT_FIELD_NAME = "support";
    private static final String METHOD_ID_FIELD_PREFIX = "method_";

    // Method name ends with '$' so as not to conflict with user method.
    private static final String INIT_METHOD_NAME = "init$";
    private static final String DISPOSE_METHOD_NAME = "dispose$";

    private static final Map<Object, StubFactory<?>> cCache;

    static {
        cCache = new SoftValuedHashMap();
    }

    /**
     * @param type
     * @param remoteInfo remote type as supported by remote server
     * @throws IllegalArgumentException if type is null or malformed
     */
    public static <R extends Remote> StubFactory<R> getStubFactory(Class<R> type,
                                                                   RemoteInfo remoteInfo)
        throws IllegalArgumentException
    {
        Object key = KeyFactory.createKey(new Object[] {type, remoteInfo});

        synchronized (cCache) {
            StubFactory<R> factory = (StubFactory<R>) cCache.get(key);
            if (factory == null) {
                factory = new StubFactoryGenerator<R>(type, remoteInfo).generateFactory();
                cCache.put(key, factory);
            }
            return factory;
        }
    }

    private final Class<R> mType;
    private final RemoteInfo mLocalInfo;
    private final RemoteInfo mRemoteInfo;

    private StubFactoryGenerator(Class<R> type, RemoteInfo remoteInfo) {
        mType = type;
        mLocalInfo = RemoteIntrospector.examine(type);
        if (remoteInfo == null) {
            remoteInfo = mLocalInfo;
        }
        mRemoteInfo = remoteInfo;
    }

    private StubFactory<R> generateFactory() {
        Class<? extends R> stubClass = generateStub();

        // Prepare identifiers for init method.
        Identifier[] ids = new Identifier[mRemoteInfo.getRemoteMethods().size()];
        int methodOrdinal = -1;
        for (RemoteMethod method : mRemoteInfo.getRemoteMethods()) {
            methodOrdinal++;
            ids[methodOrdinal] = method.getMethodID();
        }

        try {
            // Call static method to initialize method identifiers.
            stubClass.getMethod(INIT_METHOD_NAME, Identifier[].class).invoke(null, (Object) ids);

            return new Factory<R>(mType, stubClass);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        } catch (InvocationTargetException e) {
            throw new Error(e);
        } catch (NoSuchMethodException e) {
            NoSuchMethodError nsme = new NoSuchMethodError();
            nsme.initCause(e);
            throw nsme;
        }
    }

    private Class<? extends R> generateStub() {
        ClassInjector ci =
            ClassInjector.create(mType.getName() + "$Stub", mType.getClassLoader());

        ClassFile cf = new ClassFile(ci.getClassName());
        cf.addInterface(mType);
        cf.setSourceFile(StubFactoryGenerator.class.getName());
        cf.markSynthetic();
        cf.setTarget("1.5");

        final TypeDesc remoteType = TypeDesc.forClass(mType);
        final TypeDesc identifierType = TypeDesc.forClass(Identifier.class);
        final TypeDesc identifierArrayType = identifierType.toArrayType();
        final TypeDesc stubSupportType = TypeDesc.forClass(StubSupport.class);
        final TypeDesc remoteConnectionType = TypeDesc.forClass(RemoteConnection.class);
        final TypeDesc remoteInType = TypeDesc.forClass(RemoteInput.class);
        final TypeDesc remoteOutType = TypeDesc.forClass(RemoteOutput.class);
        final TypeDesc classType = TypeDesc.forClass(Class.class);
        final TypeDesc methodType = TypeDesc.forClass(Method.class);
        final TypeDesc noSuchObjectExType = TypeDesc.forClass(NoSuchObjectException.class);
        final TypeDesc remoteExType = TypeDesc.forClass(RemoteException.class);
        final TypeDesc unimplementedExType = TypeDesc.forClass(UnimplementedMethodException.class);

        // Add fields
        {
            cf.addField(Modifiers.PRIVATE.toVolatile(true).toTransient(true),
                        STUB_SUPPORT_FIELD_NAME, stubSupportType);

            int methodOrdinal = -1;
            for (RemoteMethod method : mRemoteInfo.getRemoteMethods()) {
                methodOrdinal++;
                cf.addField(Modifiers.PRIVATE.toStatic(true),
                            METHOD_ID_FIELD_PREFIX + methodOrdinal, identifierType);
            }
        }

        // Add static method to assign identifiers.
        {
            MethodInfo mi = cf.addMethod
                (Modifiers.PUBLIC.toStatic(true), INIT_METHOD_NAME,
                 null, new TypeDesc[] {identifierArrayType});

            CodeBuilder b = new CodeBuilder(mi);

            int methodOrdinal = -1;
            for (RemoteMethod method : mRemoteInfo.getRemoteMethods()) {
                methodOrdinal++;

                if (methodOrdinal == 0) {
                    // Crude check to ensure init is called at most once.
                    b.loadStaticField(METHOD_ID_FIELD_PREFIX + methodOrdinal, identifierType);
                    Label doInit = b.createLabel();
                    b.ifNullBranch(doInit, true);

                    b.newObject(TypeDesc.forClass(IllegalStateException.class));
                    b.dup();
                    b.invokeConstructor(TypeDesc.forClass(IllegalStateException.class), null);
                    b.throwObject();

                    doInit.setLocation();
                }

                b.loadLocal(b.getParameter(0));
                b.loadConstant(methodOrdinal);
                b.loadFromArray(identifierType);
                b.storeStaticField(METHOD_ID_FIELD_PREFIX + methodOrdinal, identifierType);
            }

            b.returnVoid();
        }

        // Add constructor
        {
            MethodInfo mi = cf.addConstructor
                (Modifiers.PUBLIC, new TypeDesc[] {stubSupportType});

            CodeBuilder b = new CodeBuilder(mi);

            b.loadThis();
            b.invokeSuperConstructor(null);

            b.loadThis();
            b.loadLocal(b.getParameter(0));
            b.storeField(STUB_SUPPORT_FIELD_NAME, stubSupportType);

            b.returnVoid();
        }

        // Add dispose method.
        {
            MethodInfo mi = cf.addMethod(Modifiers.PUBLIC, DISPOSE_METHOD_NAME,
                                         TypeDesc.BOOLEAN, null);
            mi.setModifiers(mi.getModifiers().toSynchronized(true));
            mi.addException(remoteExType);

            CodeBuilder b = new CodeBuilder(mi);

            b.loadThis();
            b.loadField(STUB_SUPPORT_FIELD_NAME, stubSupportType);
            Label alreadyDisposed = b.createLabel();
            b.ifNullBranch(alreadyDisposed, true);

            b.loadThis();
            b.loadField(STUB_SUPPORT_FIELD_NAME, stubSupportType);
            b.invokeInterface(stubSupportType, "dispose", null, null);

            b.loadThis();
            b.loadNull();
            b.storeField(STUB_SUPPORT_FIELD_NAME, stubSupportType);
            b.loadConstant(true);
            b.returnValue(TypeDesc.BOOLEAN);

            alreadyDisposed.setLocation();
            b.loadConstant(false);
            b.returnValue(TypeDesc.BOOLEAN);
        }

        // Implement all methods provided by server, including ones not defined
        // by local interface. This allows the server to upgrade before the
        // client, making new methods available via reflection.

        int methodOrdinal = -1;
        for (RemoteMethod method : mRemoteInfo.getRemoteMethods()) {
            methodOrdinal++;

            TypeDesc returnDesc = CodeBuilderUtil.getTypeDesc(method.getReturnType());
            TypeDesc[] paramDescs = CodeBuilderUtil.getTypeDescs(method.getParameterTypes());

            MethodInfo mi = cf.addMethod
                (Modifiers.PUBLIC, method.getName(), returnDesc, paramDescs);

            TypeDesc[] exceptionDescs = CodeBuilderUtil.getTypeDescs(method.getExceptionTypes());
            for (TypeDesc desc : exceptionDescs) {
                mi.addException(desc);
            }

            CodeBuilder b = new CodeBuilder(mi);

            LocalVariable stubSupportVar = b.createLocalVariable(null, stubSupportType);
            b.loadThis();
            b.loadField(STUB_SUPPORT_FIELD_NAME, stubSupportType);
            b.storeLocal(stubSupportVar);

            b.loadLocal(stubSupportVar);
            Label notDisposed = b.createLabel();
            b.ifNullBranch(notDisposed, false);

            // Stub has been disposed, so throw NoSuchObjectException.
            b.newObject(noSuchObjectExType);
            b.dup();
            b.loadConstant("Remote object disposed");
            b.invokeConstructor(noSuchObjectExType, new TypeDesc[] {TypeDesc.STRING});
            b.throwObject();

            notDisposed.setLocation();

            Label tryStart = b.createLabel().setLocation();

            // Create connection for invoking remote method.
            b.loadLocal(stubSupportVar);
            b.loadStaticField(METHOD_ID_FIELD_PREFIX + methodOrdinal, identifierType);
            b.invokeInterface(stubSupportType, "invoke", remoteConnectionType,
                              new TypeDesc[] {identifierType});
            LocalVariable conVar = b.createLocalVariable(null, remoteConnectionType);
            b.storeLocal(conVar);

            if (paramDescs.length > 0) {
                // TODO: Determine if no object parameters are shared. If same
                // object type is used more than once, then it might be shared.
                // In that case, always use object writing methods, which
                // forces the use of ObjectOutputStream.

                // Write parameters to connection.
                b.loadLocal(conVar);
                b.invokeInterface(remoteConnectionType, "getRemoteOutput", remoteOutType, null);
                LocalVariable remoteOutVar = b.createLocalVariable(null, remoteOutType);
                b.storeLocal(remoteOutVar);

                int i = 0;
                for (RemoteParameter paramType : method.getParameterTypes()) {
                    b.loadLocal(b.getParameter(i++));
                    CodeBuilderUtil.writeParam(b, paramType, remoteOutVar);
                }
            }

            b.loadLocal(conVar);
            if (method.isAsynchronous()) {
                b.invokeInterface(remoteConnectionType, "close", null, null);
                if (returnDesc != null) {
                    // Asynchronous method should not have a return value, but
                    // this one does for some reason. Just return 0, false, or null.
                    switch (returnDesc.getTypeCode()) {
                    case TypeDesc.BYTE_CODE:
                    case TypeDesc.SHORT_CODE:
                    case TypeDesc.CHAR_CODE:
                    case TypeDesc.INT_CODE:
                        b.loadConstant(0);
                        break;
                    case TypeDesc.LONG_CODE:
                        b.loadConstant(0L);
                        break;
                    case TypeDesc.FLOAT_CODE:
                        b.loadConstant(0.0f);
                        break;
                    case TypeDesc.DOUBLE_CODE:
                        b.loadConstant(0.0d);
                        break;
                    case TypeDesc.BOOLEAN_CODE:
                        b.loadConstant(false);
                        break;
                    default:
                        b.loadNull();
                        break;
                    }
                }
            } else {
                b.invokeInterface(remoteConnectionType, "flush", null, null);
                
                // Read response.
                b.loadLocal(conVar);
                b.invokeInterface(remoteConnectionType, "getRemoteInput", remoteInType, null);
                LocalVariable remoteInVar = b.createLocalVariable(null, remoteInType);
                b.storeLocal(remoteInVar);

                b.loadLocal(remoteInVar);
                b.invokeInterface(remoteInType, "readOk", TypeDesc.BOOLEAN, null);

                if (returnDesc != TypeDesc.BOOLEAN) {
                    b.pop();
                    if (returnDesc != null) {
                        CodeBuilderUtil.readParam(b, method.getReturnType(), remoteInVar);
                    }
                }

                // Assume server has closed connection.
            }

            if (returnDesc == null) {
                b.returnVoid();
            } else {
                b.returnValue(returnDesc);
            }

            Label tryEnd = b.createLabel().setLocation();

            // Convert any IOException to a RemoteException.

            b.exceptionHandler(tryStart, tryEnd, RemoteException.class.getName());
            // RemoteException is an IOException, but leave it as-is.
            b.throwObject();

            b.exceptionHandler(tryStart, tryEnd, IOException.class.getName());
            LocalVariable exceptionVar =
                b.createLocalVariable(null, TypeDesc.forClass(IOException.class));
            b.storeLocal(exceptionVar);

            b.newObject(remoteExType);
            b.dup();
            b.loadLocal(exceptionVar);
            b.invokeVirtual(TypeDesc.forClass(Throwable.class), "getMessage",
                            TypeDesc.STRING, null);
            b.loadLocal(exceptionVar);
            b.invokeConstructor(remoteExType, new TypeDesc[] {TypeDesc.STRING,
                                                              TypeDesc.forClass(Throwable.class)});
            b.throwObject();
        }

        // Methods unimplemented by server throw UnimplementedMethodException

        for (RemoteMethod method : mLocalInfo.getRemoteMethods()) {
            List<? extends RemoteParameter> paramList = method.getParameterTypes();
            try {
                RemoteParameter[] paramTypes = new RemoteParameter[paramList.size()];
                paramList.toArray(paramTypes);
                mRemoteInfo.getRemoteMethod(method.getName(), paramTypes);
                // Method has been implemented.
                continue;
            } catch (NoSuchMethodException e) {
                // Server does not have this method.
            }

            TypeDesc returnDesc = CodeBuilderUtil.getTypeDesc(method.getReturnType());
            TypeDesc[] paramDescs = CodeBuilderUtil.getTypeDescs(method.getParameterTypes());

            MethodInfo mi = cf.addMethod
                (Modifiers.PUBLIC, method.getName(), returnDesc, paramDescs);

            TypeDesc[] exceptionDescs = CodeBuilderUtil.getTypeDescs(method.getExceptionTypes());
            for (TypeDesc desc : exceptionDescs) {
                mi.addException(desc);
            }

            CodeBuilder b = new CodeBuilder(mi);
            
            b.newObject(unimplementedExType);
            b.dup();
            b.loadConstant(TypeDesc.forClass(mType));
            b.loadConstant(method.getName());
            b.loadConstant(paramList.size());
            b.newObject(classType.toArrayType());
            for (int i=0; i<paramList.size(); i++) {
                b.dup();
                b.loadConstant(i);
                b.loadConstant(CodeBuilderUtil.getTypeDesc(paramList.get(i)));
                b.storeToArray(classType);
            }
            b.invokeVirtual(classType, "getMethod", methodType,
                            new TypeDesc[] {TypeDesc.STRING, classType.toArrayType()});
            b.invokeConstructor(unimplementedExType, new TypeDesc[] {methodType});
            b.throwObject();
        }

        return ci.defineClass(cf);
    }

    private static class Factory<R extends Remote> implements StubFactory<R> {
        private final Class<R> mType;
        private final Constructor<? extends R> mStubCtor;
        private final Method mDisposeMethod;

        Factory(Class<R> type, Class<? extends R> stubClass) throws NoSuchMethodException {
            mType = type;
            mStubCtor = stubClass.getConstructor(StubSupport.class);
            mDisposeMethod = stubClass.getMethod(DISPOSE_METHOD_NAME, (Class[]) null);
        }

        public Class<R> getRemoteType() {
            return mType;
        }

        public Class<? extends R> getStubClass() {
            return mStubCtor.getDeclaringClass();
        }

        public R createStub(StubSupport support) {
            Throwable error;
            try {
                return mStubCtor.newInstance(support);
            } catch (InstantiationException e) {
                error = e;
            } catch (IllegalAccessException e) {
                error = e;
            } catch (InvocationTargetException e) {
                error = e.getCause();
            }
            InternalError ie = new InternalError();
            ie.initCause(error);
            throw ie;
        }

        public boolean isStub(R stub) {
            return stub != null && getStubClass().isInstance(stub);
        }

        public boolean dispose(R stub) throws RemoteException {
            try {
                return (Boolean) mDisposeMethod.invoke(stub, (Object[]) null);
            } catch (IllegalArgumentException e) {
                // Assume R is not a valid stub
            } catch (IllegalAccessException e) {
                // Should not happen
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                if (cause instanceof Error) {
                    throw (Error) cause;
                }
                if (cause instanceof RemoteException) {
                    throw (RemoteException) cause;
                }
                throw new RemoteException(cause.getMessage(), cause);
            }
            return false;
        }
    }
}
