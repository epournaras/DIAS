#!/bin/bash

for i in summaries/*.dat
do
	python plot.py $i &
done
