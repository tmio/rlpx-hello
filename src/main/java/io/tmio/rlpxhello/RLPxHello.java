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
import java.security.Security;
import java.util.Collections;

import io.vertx.core.Vertx;
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.CompletableAsyncCompletion;
import net.consensys.cava.crypto.SECP256K1;
import net.consensys.cava.rlpx.RLPxService;
import net.consensys.cava.rlpx.vertx.VertxRLPxService;
import net.consensys.cava.rlpx.wire.SubProtocol;
import net.consensys.cava.rlpx.wire.SubProtocolHandler;
import net.consensys.cava.rlpx.wire.SubProtocolIdentifier;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.logl.Level;
import org.logl.Logger;
import org.logl.LoggerProvider;
import org.logl.logl.SimpleLogger;

public class RLPxHello {

  private static void displayUsage(Logger logger) {
    logger.info("rlpx-hello enodeid host port");
    logger.info("Example: rlpx-hello 7A8FBB31BFF7C48179F8504B047313EBB7446A0233175FFDA6EB4C27AAA5D2AEDCEF4DD9501B4F17B4F16588F0FD037F9B9416B8CACA655BEE3B14B4EF67441A 127.0.0.1 30303");
    System.exit(1);
  }

  public static void main(String[] args) {
    Security.addProvider(new BouncyCastleProvider());

    LoggerProvider loggerProvider = SimpleLogger.withLogLevel(Level.DEBUG).toPrintWriter(
        new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out, UTF_8))));
    Logger errorLogger = loggerProvider.getLogger("err");
    Logger usageLogger = loggerProvider.getLogger("usage");

    for (String arg : args) {
      if ("-h".equals(arg) || "--help".equals(arg)) {
        displayUsage(usageLogger);
      }
    }
    if (args.length != 3) {
      displayUsage(usageLogger);
    }

    SECP256K1.PublicKey peerPublicKey = null;
    InetSocketAddress peerAddress = null;
    try {
      peerPublicKey = SECP256K1.PublicKey.fromHexString(args[0]);
      peerAddress = InetSocketAddress.createUnresolved(args[1], Integer.parseInt(args[2]));
    } catch(IllegalArgumentException e) {
      errorLogger.error(e.getMessage(), e);
      displayUsage(usageLogger);
    }

    Vertx vertx = Vertx.vertx();
    try {
      SECP256K1.KeyPair ourKeyPair = SECP256K1.KeyPair.random();

      VertxRLPxService service = new VertxRLPxService(
          vertx,
          loggerProvider,
          45000,
          "0.0.0.0",
          45000,
          ourKeyPair,
          Collections.singletonList(new SubProtocol() {
            @Override
            public SubProtocolIdentifier id() {
              return new SubProtocolIdentifier() {
                @Override
                public String name() {
                  return "eth";
                }

                @Override
                public int version() {
                  return 63;
                }
              };
            }

            @Override
            public boolean supports(SubProtocolIdentifier subProtocolIdentifier) {
              return false;
            }

            @Override
            public int versionRange(int version) {
              return 0;
            }

            @Override
            public SubProtocolHandler createHandler(RLPxService service) {
              return null;
            }
          }),
          "RLPxHello 0.1.0");
      try {
        service.start().join();
        AsyncCompletion completion = service.connectTo(peerPublicKey, peerAddress);
        completion.join();

      } catch (InterruptedException e) {
        errorLogger.error(e.getMessage(), e);
      } finally {
        try {
          service.stop().join();
        } catch (InterruptedException e) {
          errorLogger.error(e.getMessage(), e);
        }
      }
    } finally {
      CompletableAsyncCompletion result = AsyncCompletion.incomplete();
      vertx.close(handler -> {
        if (handler.failed()) {
          result.completeExceptionally(handler.cause());
        } else {
          result.complete();
        }
      });
      try {
        result.join();
      } catch (Exception e) {
        errorLogger.error(e.getMessage(), e);
      }
    }
  }
}
