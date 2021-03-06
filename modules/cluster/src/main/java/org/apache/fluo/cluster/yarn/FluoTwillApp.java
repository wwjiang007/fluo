/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package org.apache.fluo.cluster.yarn;

import java.io.File;

import org.apache.fluo.api.config.FluoConfiguration;
import org.apache.twill.api.ResourceSpecification;
import org.apache.twill.api.ResourceSpecification.SizeUnit;
import org.apache.twill.api.TwillApplication;
import org.apache.twill.api.TwillSpecification;
import org.apache.twill.api.TwillSpecification.Builder.LocalFileAdder;
import org.apache.twill.api.TwillSpecification.Builder.MoreFile;
import org.apache.twill.api.TwillSpecification.Builder.MoreRunnable;
import org.apache.twill.api.TwillSpecification.Builder.RunnableSetter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents Fluo application in Twill
 */
@Deprecated
public class FluoTwillApp implements TwillApplication {

  private static final Logger log = LoggerFactory.getLogger(FluoTwillApp.class);

  private final FluoConfiguration config;
  private final String fluoConf;

  public FluoTwillApp(FluoConfiguration config, String fluoConf) {
    this.config = config;
    this.fluoConf = fluoConf;
  }

  private MoreFile addConfigFiles(LocalFileAdder fileAdder) {
    File confDir = new File(fluoConf);
    MoreFile moreFile = null;
    File[] confFiles = confDir.listFiles();
    if (confFiles != null) {
      for (File f : confFiles) {
        if (f.isFile()) {
          log.trace("Adding config file - " + f.getAbsolutePath());
          if (moreFile == null) {
            moreFile = fileAdder.add(String.format("./conf/%s", f.getName()), f);
          } else {
            moreFile = moreFile.add(String.format("./conf/%s", f.getName()), f);
          }
        }
      }
    }
    return moreFile;
  }

  @Override
  public TwillSpecification configure() {

    final int oracleInstances =
        org.apache.fluo.cluster.util.FluoYarnConfig.getOracleInstances(config);
    final int oracleMaxMemory =
        org.apache.fluo.cluster.util.FluoYarnConfig.getOracleMaxMemory(config);
    final int oracleNumCores =
        org.apache.fluo.cluster.util.FluoYarnConfig.getOracleNumCores(config);
    final int workerInstances =
        org.apache.fluo.cluster.util.FluoYarnConfig.getWorkerInstances(config);
    final int workerMaxMemory =
        org.apache.fluo.cluster.util.FluoYarnConfig.getWorkerMaxMemory(config);
    final int workerNumCores =
        org.apache.fluo.cluster.util.FluoYarnConfig.getWorkerNumCores(config);

    log.info(
        "Configuring Fluo '{}' application with {} Oracle instances and {} Worker instances "
            + "with following properties:",
        config.getApplicationName(), oracleInstances, workerInstances);

    log.info("{} = {}", org.apache.fluo.cluster.util.FluoYarnConfig.ORACLE_MAX_MEMORY_MB_PROP,
        oracleMaxMemory);
    log.info("{} = {}", org.apache.fluo.cluster.util.FluoYarnConfig.WORKER_MAX_MEMORY_MB_PROP,
        workerMaxMemory);
    log.info("{} = {}", org.apache.fluo.cluster.util.FluoYarnConfig.ORACLE_NUM_CORES_PROP,
        oracleNumCores);
    log.info("{} = {}", org.apache.fluo.cluster.util.FluoYarnConfig.WORKER_NUM_CORES_PROP,
        workerNumCores);

    // Start building Fluo Twill application
    MoreRunnable moreRunnable =
        TwillSpecification.Builder.with().setName(org.apache.fluo.cluster.runner.YarnAppRunner
            .getYarnApplicationName(config.getApplicationName())).withRunnable();

    // Configure Oracle(s)
    ResourceSpecification oracleResources =
        ResourceSpecification.Builder.with().setVirtualCores(oracleNumCores)
            .setMemory(oracleMaxMemory, SizeUnit.MEGA).setInstances(oracleInstances).build();

    LocalFileAdder fileAdder = moreRunnable
        .add(org.apache.fluo.cluster.runnable.OracleRunnable.ORACLE_NAME,
            new org.apache.fluo.cluster.runnable.OracleRunnable(), oracleResources)
        .withLocalFiles();
    RunnableSetter runnableSetter = addConfigFiles(fileAdder).apply();

    // Configure Worker(s)
    ResourceSpecification workerResources =
        ResourceSpecification.Builder.with().setVirtualCores(workerNumCores)
            .setMemory(workerMaxMemory, SizeUnit.MEGA).setInstances(workerInstances).build();

    fileAdder = runnableSetter
        .add(org.apache.fluo.cluster.runnable.WorkerRunnable.WORKER_NAME,
            new org.apache.fluo.cluster.runnable.WorkerRunnable(), workerResources)
        .withLocalFiles();
    runnableSetter = addConfigFiles(fileAdder).apply();

    // Set runnable order, build and return TwillSpecification
    return runnableSetter.withOrder()
        .begin(org.apache.fluo.cluster.runnable.OracleRunnable.ORACLE_NAME)
        .nextWhenStarted(org.apache.fluo.cluster.runnable.WorkerRunnable.WORKER_NAME).build();
  }
}
