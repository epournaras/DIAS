#!/bin/bash

folder=dump/live_30n_600s/
nodes=30
runtime=600

#args= folder, ID, Port (port0=auto)
java -Dvar=live0 -cp ../lib/*:../build/classes/ protocols.DIASLiveExperiment $folder 0 5555 &
sleep 1

i=1
while [ $i -le $nodes ]; do
    java -Dvar=live$i -cp ../lib/*:../build/classes/ protocols.DIASLiveExperiment $folder $i 0 &
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
 
