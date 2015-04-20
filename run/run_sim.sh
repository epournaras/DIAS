#!/bin/bash
folder=dump/sim2_10-100delay_0loss/
mindelay=10
maxdelay=100
losses_fraction=0.0

java -Xmx6G -Dvar=sim -cp ../lib/*:../build/classes/ protocols.DIASLossExperiment $folder $mindelay $maxdelay $losses_fraction
