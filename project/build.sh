#!/usr/bin/env bash

set -o xtrace
set -o errexit
set -o nounset
set -o pipefail

echo "Building ServicePlus project using sbt"
date
sbt clean test dockerPackage
date
