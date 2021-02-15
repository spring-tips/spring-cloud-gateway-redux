#!/usr/bin/env bash
while true; do
  curl localhost:9292/hi  -u jlong:pw -v
done