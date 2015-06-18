#!/bin/bash

find dump/* -type d | while read i
do
	echo $i
	outname=summaries/$(basename $i).dat
	if [ ! -e "$outname" ]; then
		java -cp ../lib/*:../build/classes/ protocols.DIASLogReplayer "$i" | tail -n+4 > "$outname"
	fi
done
