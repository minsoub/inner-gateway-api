#!/bin/bash

export CLASSPATH=$CLASSPATH:.

echo "profile0 => $1"
echo "profile1 => $2"
echo "profile2 => $3"
echo "profile3 => $4"


export AWS_ACCESS_KEY_ID=$2
export AWS_SECRET_ACCESS_KEY=$3

echo $AWS_ACCESS_KEY_ID
echo $AWS_SECRET_ACCESS_KEY

java -Dspring.profiles.active=$1 -Dreactor.netty.pool.leasingStrategy=lifo -jar ./inner-gateway-api.jar