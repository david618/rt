#!/bin/bash

namebase="tk"

for i in $(seq 1 $1); do 

name=${namebase}${i}

dcos marathon app remove ${name}

done
