#!/bin/bash

if [ "$MATRIX_JOB_TYPE" == "TEST" ]; then
    set -o errexit

    sudo apt update -y

    echo "Upgrading Docker..."
    sudo apt install --only-upgrade docker-ce -y
    echo "Upgrade Done"

    docker info

    for server in ci/run-*-server.sh; do
        chmod -R 777 ci
        ./$server
    done
fi


