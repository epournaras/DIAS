#!/bin/bash

nodes=20
runtime=650
folder=dump/distr_${nodes}n_${runtime}s/
ip=$(grep $(hostname) /etc/hosts | awk '{print $1}')
echo $ip
sed -i "s/peerZeroIP=.*/peerZeroIP=$ip/" conf/protopeer.conf

#args= folder, ID, Port (port0=auto)
java -Dvar=distr0 -cp ../lib/*:../build/classes/:../lib/unused/ProtoPeer.jar protocols.DIASLiveExperiment $folder 0 5555 &

i=1
while [ $i -le $nodes ]; do
    bsub -R rusage[mem=2014] java -Xmx1G -Dvar=distr$i -cp ../lib/*:../build/classes/:../lib/unused/ProtoPeer.jar protocols.DIASLiveExperiment $folder $i 0 
    i=$((i+1))
done      

echo "WAITING $runtime s (STOP WITH ENTER)"
read -t $runtime

kill %1
bkill $(bjobs -w | grep "protocols.DIASLiveExperiment" | awk '{print $1}')
