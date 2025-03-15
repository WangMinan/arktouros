#!/bin/bash
systemctl stop arktouros
systemctl stop elasticsearch
rm -rf /var/lib/elasticsearch/node.lock
rm -rf /var/lib/elasticsearch/snapshot_cache/write.lock
