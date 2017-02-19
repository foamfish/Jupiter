/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jupiter.common.concurrent;

import org.jupiter.common.util.JConstants;
import org.jupiter.common.util.JvmTools;
import org.jupiter.common.util.StackTraceUtil;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Jupiter
 * org.jupiter.common.concurrent
 *
 * @author jiachun.fjc
 */
public abstract class AbstractRejectedExecutionHandler implements RejectedExecutionHandler {

    protected static final InternalLogger logger = InternalLoggerFactory.getInstance(AbstractRejectedExecutionHandler.class);

    private static final ExecutorService dumpExecutor = Executors.newSingleThreadExecutor();

    protected final String threadPoolName;
    protected final AtomicBoolean dumpNeeded;

    public AbstractRejectedExecutionHandler(String threadPoolName, boolean dumpNeeded) {
        this.threadPoolName = threadPoolName;
        this.dumpNeeded = new AtomicBoolean(dumpNeeded);
    }

    public void dumpJvmInfo() {
        if (dumpNeeded.getAndSet(false)) {
            dumpExecutor.execute(new Runnable() {

                @Override
                public void run() {
                    FileOutputStream stackOutput = null;
                    try {
                        stackOutput = new FileOutputStream(new File("jupiter.dump_" + threadPoolName + ".log"));
                        List<String> stacks = JvmTools.jStack();
                        for (String s : stacks) {
                            stackOutput.write(s.getBytes(JConstants.UTF8));
                        }

                        if (JvmTools.memoryUsed() > 0.9) {
                            JvmTools.jMap("jupiter.dump_" + threadPoolName + ".bin", false);
                        }
                    } catch (Throwable t) {
                        logger.error("Dump jvm info error: {}.", StackTraceUtil.stackTrace(t));
                    } finally {
                        if (stackOutput != null) {
                            try {
                                stackOutput.close();
                            } catch (IOException ignored) {}
                        }
                    }
                }
            });
        }
    }
}