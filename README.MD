RLPx Hello
==========

This small utility allows developers to connect to a remote RLPx endpoint and exchange a HELLO message with it.

It logs the hello messages sent and received, allowing to debug the current capabilities of the remote host.

# Building

RLPx-hello requires Java 11.

Run `./gradlew build`

# Running the application

After building the application, unzip the zip file under build/distributions.

You can run rlpx-hello with:
`
bin/rlpx-hello <remote-peer-pubkey> <peer-host> <peer-port>
`

Note the peer's remote pub key should be encoded, just like it's given for enode format.

# Contributions

Feel free to fork, open issues and PRs, and hack away. The code is under Apache Software License v2.

