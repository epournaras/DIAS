#!/bin/bash

find ../PeerSamplingService -name *.java > peer_sampling_service_sources
find src -name *.java > dias_sources

cat dias_sources peer_sampling_service_sources > all_sources

javac -d build/classes -sourcepath src -cp lib/*:build/classes/:lib/unused/ProtoPeer.jar @all_sources