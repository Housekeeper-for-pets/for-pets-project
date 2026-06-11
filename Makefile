.PHONY: multi-up multi-down

PROMETHEUS_MULTI_CONFIG := ./monitoring/prometheus-multi-local.yml
MULTI_SERVICES := mysql redis kafka qdrant app-blue app-green nginx-multi prometheus grafana

multi-up:
	PROMETHEUS_CONFIG=$(PROMETHEUS_MULTI_CONFIG) docker compose --profile multi up -d $(MULTI_SERVICES)

multi-down:
	docker compose --profile multi --profile local-proxy down -v --remove-orphans
