package io.levelops.controlplane.discovery;

import lombok.extern.slf4j.Slf4j;

import org.springframework.integration.support.locks.ExpirableLockRegistry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

@Slf4j
public class LocalLockRegistry implements ExpirableLockRegistry{

  @Override
  public Lock obtain(Object lockKey) {
      return new Lock(){

          @Override
          public void lock() {
              log.info("local lock requested...");
          }

          @Override
          public void lockInterruptibly() throws InterruptedException {
              log.info("local lockInterruptibly requested...");
          }

          @Override
          public boolean tryLock() {
              log.info("local tryLock requested...");
              return true;
          }

          @Override
          public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
              log.info("local tryLock requested...");
              return true;
          }

          @Override
          public void unlock() {
              log.info("local unlock requested...");
          }

          @Override
          public Condition newCondition() {
              // TODO Auto-generated method stub
              throw new UnsupportedOperationException("Unimplemented method 'newCondition'");
          }

      };
  }

  @Override
  public void expireUnusedOlderThan(long age) {
      log.info("local expireUnunsedOlderThan requested...");
  }
  
}
