#!/bin/bash

nodes=10
runtime=40
folder=dump/live_${nodes}nodes_${runtime}s/

#args= folder, ID, Port (port0=auto)
rm -r $folder
mkdir -p $folder
java -Dvar=$(basename $folder)_0 -cp ../lib/*:../build/classes/ protocols.DIASLiveExperiment $folder 0 5555 &
sleep 1

i=1
while [ $i -le $nodes ]; do
    java -Dvar=$(basename $folder)_$i -cp ../lib/*:../build/classes/ protocols.DIASLiveExperiment $folder $i 0 &
    i=$((i+1))
done      
sleep 10

echo LETTING IT RUN FOR $runtime SECONDS
for i in $(seq $runtime 1);
do 
	echo $i
	sleep 1
done

echo ENDING
pkill java
sleep 1s

java -cp ../lib/*:../build/classes/ protocols.DIASLogReplayer $folder | tail -n+4 > summaries/$(basename $folder).dat 
python plot.py summaries/$(basename $folder).dat
