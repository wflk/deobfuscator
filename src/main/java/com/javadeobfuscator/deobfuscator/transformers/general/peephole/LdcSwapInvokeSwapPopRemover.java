/*
 * Copyright 2016 Sam Sun <me@samczsun.com>
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.javadeobfuscator.deobfuscator.transformers.general.peephole;

import com.javadeobfuscator.deobfuscator.org.objectweb.asm.Opcodes;
import com.javadeobfuscator.deobfuscator.org.objectweb.asm.tree.AbstractInsnNode;
import com.javadeobfuscator.deobfuscator.org.objectweb.asm.tree.MethodInsnNode;
import com.javadeobfuscator.deobfuscator.transformers.Transformer;
import com.javadeobfuscator.deobfuscator.utils.Utils;
import com.javadeobfuscator.deobfuscator.utils.WrappedClassNode;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class LdcSwapInvokeSwapPopRemover extends Transformer {
    public LdcSwapInvokeSwapPopRemover(Map<String, WrappedClassNode> classes, Map<String, WrappedClassNode> classpath) {
        super(classes, classpath);
    }

    @Override
    public void transform() throws Throwable {
        AtomicInteger counter = new AtomicInteger();
        classNodes().stream().map(wrappedClassNode -> wrappedClassNode.classNode).forEach(classNode -> {
            classNode.methods.stream().filter(methodNode -> methodNode.instructions.getFirst() != null).forEach(methodNode -> {
                boolean modified = false;
                do {
                    modified = false;
                    for (int i = 0; i < methodNode.instructions.size(); i++) {
                        AbstractInsnNode node = methodNode.instructions.get(i);
                        if (Utils.willPushToStack(node.getOpcode())) {
                            AbstractInsnNode next = Utils.getNext(node);
                            if (next.getOpcode() == Opcodes.SWAP) {
                                AbstractInsnNode swap = next;
                                next = Utils.getNext(next);
                                if (next instanceof MethodInsnNode) {
                                    MethodInsnNode methodInsnNode = (MethodInsnNode) next;
                                    if (methodInsnNode.desc.equals("(Ljava/lang/String;)Ljava/lang/String;")) { //Lazy
                                        AbstractInsnNode next1 = Utils.getNext(next);
                                        if (next1.getOpcode() == Opcodes.SWAP && next1.getNext().getOpcode() == Opcodes.POP) {
                                            methodNode.instructions.remove(next1.getNext());
                                            methodNode.instructions.remove(next1);
                                            methodNode.instructions.remove(swap);
                                            methodNode.instructions.remove(node);
                                            counter.incrementAndGet();
                                            modified = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } while (modified);
            });
        });
        System.out.println("Removed " + counter.get() + " ldc-swap-invoke-swap-pop patterns");
    }
}
