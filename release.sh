#!/usr/bin/env bash
# $Id$

mvn -T 1 -B -P+release clean release:clean release:prepare release:perform
