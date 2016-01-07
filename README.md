# testclj

## Note to self regarding setup
So this is a project using Storm. I want to collect data via a REST API, do some computation and return a result to some web ui, ideally via sockets.
There are going to be two main components - a web server to collect REST responses, and manage the socket to the web UI. The computation will be performed by Storm. The two components can communicate via distributed RPC.
I'm going to need state, eventually, and DRPC is best done via Trident, so we have an extra player. As it happens, Trident looks fairly easy to use, and to talk to it via Clojure (because why not), we're going to use a library called [Marceline](https://github.com/yieldbot/marceline).

# Deployment instructions
Note that although these are cluster instructions, I'm actually only running on one machine currently.
 0. Set up a machine(s) ready to become a storm cluster.
  - Make sure the necessary inbound ports are open. In particular you'll want ports open for at least SSH (22), HTTP (80), Storm UI (8080) and DRPC (3772). AWS doesn't do anything if the port is closed, just hangs...
  - If you're using AWS, remember to connect using the public DNS - IP didn't seem to work.
  - Install JDK and Python
 1. Set up a Storm cluster ready to run our code. Instructions from [here](http://storm.apache.org/documentation/Setting-up-a-Storm-cluster.html)
  a) Set up Zookeeper according to [this](http://zookeeper.apache.org/doc/r3.3.3/zookeeperStarted.html#sc_InstallingSingleMode)
  b) Download a Storm release from [here](http://storm.apache.org/downloads.html). It should probably match the codebase? Not sure, but I'd go for 0.10.0 to be safe.
  c) Extract somewhere, and create a file conf/storm.yaml containing the following:
    
      storm.zookeeper.servers:
          - "localhost"
      storm.local.dir: "~/"
      nimbus.host: "localhost"
      
      ## Locations of the drpc servers
      drpc.servers:
          - "localhost"

  Obviously if you're running multiple machines in your cluster things might look a little different, but the principle should be the same.
  
  d) Run the various storm components. I did this using `screen` so that I could see the output of each command separately and restart them in-place if necessary. For production you'd want a script and a supervisor to revive them when they die. The four components I ran (I don't know if all are necessary) are:
  
      $ ./bin/storm nimbus
      $ ./bin/storm supervisor
      $ ./bin/storm ui
      $ ./bin/storm drpc
  
  At this point the cluster should be ready.
 3. Now we need to deploy the code we wrote so that we can have fun output! To do that, we need to package it up into a JAR. Leiningen does that with `lein uberjar`. We need to specify a main class though, which is currently `main.java.testclj.marceline-test`. Note that there is some confusion because our Clojure namespace contains a hyphen, which turns into an underscore in the Java class. Also I manually deleted the automatically-bundled Storm dependencies, but I don't know if that was critical. The self-contained JAR (with SNAPSHOT in the name) can now be `scp`'d over to the main cluster. 
 4. Finally do the deployment: `bin/storm jar ../testclj-0.1.0-SNAPSHOT-standalone.jar main.java.testclj.marceline_test myfoo`
 Note the underscore! Took me a while to spot that one... Also `myfoo` is the name for the topology but I don't know what it achieves beyond showing up in the web UI.
 5. Now you can run the server in ./server (currently with `node index.js` though I might switch to Python for fun). Joy!


## Installation

Download from http://example.com/FIXME.

## Usage

FIXME: explanation

    $ java -jar testclj-0.1.0-standalone.jar [args]

## Options

FIXME: listing of options this app accepts.

## Examples

...

### Bugs

...

### Any Other Sections
### That You Think
### Might be Useful

## License

Copyright © 2016 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
