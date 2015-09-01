#!/bin/bash

nodes=3
mindelay=0
maxdelay=100
losses_fraction=0
folder=dump/sim_${mindelay}-${maxdelay}delay_${nodes}n/

rm -vr $folder
mkdir -v $folder
java -Xmx6G -Dvar=$(basename $folder) -cp lib/*:build/classes/ protocols.DIASLossExperiment $folder $mindelay $maxdelay $losses_fraction

java -cp lib/*:build/classes/ protocols.DIASLogReplayer $folder | tail -n+4 > summaries/$(basename $folder).dat 
python plot.py summaries/$(basename $folder).dat
