/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.graal.python.builtins.objects.cext;

import com.oracle.graal.python.builtins.objects.cext.CExtNodes.CExtBaseNode;
import com.oracle.graal.python.builtins.objects.cext.PThreadStateMRFactory.GetTypeIDNodeGen;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.MessageResolution;
import com.oracle.truffle.api.interop.Resolve;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.Node;

@MessageResolution(receiverType = PThreadState.class)
public class PThreadStateMR {

    public static final String CUR_EXC_TYPE = "curexc_type";
    public static final String CUR_EXC_VALUE = "curexc_value";
    public static final String CUR_EXC_TRACEBACK = "curexc_traceback";
    public static final String EXC_TYPE = "exc_type";
    public static final String EXC_VALUE = "exc_value";
    public static final String EXC_TRACEBACK = "exc_traceback";
    public static final String DICT = "dict";
    public static final String PREV = "prev";

    @SuppressWarnings("unknown-message")
    @Resolve(message = "com.oracle.truffle.llvm.spi.GetDynamicType")
    abstract static class GetDynamicTypeNode extends Node {
        @Child private GetTypeIDNode getTypeIDNode;

        public Object access(@SuppressWarnings("unused") PThreadState object) {
            if (getTypeIDNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getTypeIDNode = insert(GetTypeIDNode.create());
            }
            return getTypeIDNode.execute();
        }
    }

    abstract static class GetTypeIDNode extends CExtBaseNode {

        @Child private PCallNativeNode callUnaryNode;

        @CompilationFinal private TruffleObject funGetThreadStateTypeID;

        public abstract Object execute();

        @Specialization(assumptions = "singleContextAssumption()")
        Object doByteArray(@Cached("callGetByteArrayTypeID()") Object nativeType) {
            // TODO(fa): use weak reference ?
            return nativeType;
        }

        @Specialization(replaces = "doByteArray")
        Object doByteArrayMultiCtx() {
            return callGetByteArrayTypeIDCached();
        }

        protected Object callGetByteArrayTypeID() {
            return callGetArrayTypeID(importCAPISymbol(NativeCAPISymbols.FUN_GET_THREAD_STATE_TYPE_ID));
        }

        private Object callGetByteArrayTypeIDCached() {
            if (funGetThreadStateTypeID == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                funGetThreadStateTypeID = importCAPISymbol(NativeCAPISymbols.FUN_GET_THREAD_STATE_TYPE_ID);
            }
            return callGetArrayTypeID(funGetThreadStateTypeID);
        }

        private Object callGetArrayTypeID(TruffleObject fun) {
            if (callUnaryNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                callUnaryNode = insert(PCallNativeNode.create());
            }
            return callUnaryNode.execute(fun, new Object[0]);
        }

        public static GetTypeIDNode create() {
            return GetTypeIDNodeGen.create();
        }
    }
}
