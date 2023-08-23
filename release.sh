#!/usr/bin/env bash
# $Id$

mvn -T 1 -B release:prepare release:perform
