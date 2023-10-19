#!/usr/bin/env bash
# $Id$

mvn -T 1 -B -P+deploy release:prepare release:perform
