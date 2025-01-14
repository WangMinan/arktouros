docker network create elastic

docker pull docker.elastic.co/elasticsearch/elasticsearch:8.15.0
docker run --name elasticsearch --net elastic -p 9200:9200 -it -m 4GB docker.elastic.co/elasticsearch/elasticsearch:8.15.0

docker pull docker.elastic.co/kibana/kibana:8.15.0
docker run --name kibana --net elastic -p 5601:5601 docker.elastic.co/kibana/kibana:8.15.0

#docker exec -it elasticsearch /usr/share/elasticsearch/bin/elasticsearch-reset-password -u elastic
#docker exec -it elasticsearch /usr/share/elasticsearch/bin/elasticsearch-create-enrollment-token -s kibana
