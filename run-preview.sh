#!/bin/sh
set -eu
cd "$(dirname "$0")"
java Build.java run
