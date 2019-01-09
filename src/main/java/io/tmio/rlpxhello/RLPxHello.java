/*
 * Copyright 2019 Antoine Toulme.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.tmio.rlpxhello;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.Collections;

import io.vertx.core.Vertx;
import net.consensys.cava.crypto.SECP256K1;
import net.consensys.cava.rlpx.WireConnectionRepository;
import net.consensys.cava.rlpx.vertx.VertxRLPxService;
import net.consensys.cava.rlpx.wire.WireConnection;
import org.logl.Level;
import org.logl.Logger;
import org.logl.LoggerProvider;
import org.logl.logl.SimpleLogger;

public class RLPxHello {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    LoggerProvider loggerProvider = SimpleLogger.withLogLevel(Level.DEBUG).toPrintWriter(
        new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out, UTF_8))));
    Logger errorLogger = loggerProvider.getLogger("err");
    VertxRLPxService service = new VertxRLPxService(
        vertx,
        loggerProvider,
        45000,
        "0.0.0.0",
        45000,
        SECP256K1.KeyPair.random(),
        Collections.emptyList(),
        "RLPxHello 0.1.0");
    try {
      service.start().join();
      service.connectTo(
          SECP256K1.PublicKey.fromHexString(args[0]),
          InetSocketAddress.createUnresolved(args[1], Integer.parseInt(args[2])));
      Thread.sleep(5000);
      WireConnectionRepository repo = service.repository();
      Logger logger = loggerProvider.getLogger("rlpxhello");
      for (WireConnection conn : repo.asIterable()) {
        logger.debug("Connection {}", conn);
      }

    } catch (InterruptedException e) {
      errorLogger.error(e.getMessage(), e);
    } finally {
      try {
        service.stop().join();
      } catch (InterruptedException e) {
        errorLogger.error(e.getMessage(), e);
      }
    }
  }
}
