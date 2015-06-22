
rm -vr dump/Experiment01 
mkdir -v dump/Experiment01
java -cp ../lib/*:../build/classes/ -Dvar=orig protocols.DIASApplExperiment; 

java -cp ../lib/*:../build/classes/ protocols.DIASLogReplayer dump/Experiment01/ | tail -n+4 > summaries/orig.dat; 
python plot.py summaries/orig.dat
