#!/bin/bash

nodes=20
runtime=650
folder=dump/distr_${nodes}n_${runtime}s/
ip=10.201.0.3$(hostname | cut -c7)
sed -i "s/peerZeroIP=.*/peerZeroIP=$ip/" conf/protopeer.conf

##handle aborts correctly
trap ctrlc INT
function ctrlc() {
	echo GOT CTRL+C
	pkill python
	pkill java
	bkill $(bjobs -w | grep "protocols.DIASLiveExperiment" | awk '{print $1}')
	exit 1
}

#args= folder, ID, Port (port0=auto)
echo START HEAD NODE
rm -r $folder
mkdir -p $folder
java -Dvar=live0 -cp bin/lib/*:bin/classes/ protocols.DIASLiveExperiment $folder 0 5555 &

echo START WORKERS
i=1
while [ $i -le $nodes ]; do
    bsub -R rusage[mem=2014] java -Xmx1G -Dvar=distr$i -cp bin/lib/*:bin/classes/ protocols.DIASLiveExperiment $folder $i 0 
    i=$((i+1))
done      

echo WAITING FOR WORKERS TO START
while [ "$(bjobs -w | grep "protocols.DIASLiveExperiment" | grep -c PEND)" -gt 0 ];
do
	echo .
	sleep 3
done

echo START DYNAMIC PLOT
python dynplot.py $(basename $folder) &

echo LETTING IT RUN FOR $runtime SECONDS
for i in $(seq $runtime -1 1);
do 
	echo $i
	sleep 1
done

echo TERMINATING
pkill python
pkill java
bkill $(bjobs -w | grep "protocols.DIASLiveExperiment" | awk '{print $1}')
sleep 10s

echo FINAL PLOT
java -cp bin/lib/*:bin/classes/ protocols.DIASLogReplayer $folder | tail -n+4 > summaries/$(basename $folder).dat 
python plot.py summaries/$(basename $folder).dat
