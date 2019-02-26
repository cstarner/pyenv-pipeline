#!/usr/bin/env bash

MANAGED_VIRTUALENV_WITH_SPACES='/var/managed virtualenv with spaces'

mkdir "$MANAGED_VIRTUALENV_WITH_SPACES"
cd "$MANAGED_VIRTUALENV_WITH_SPACES"
python3.4 -m virtualenv --python=python3.4 .