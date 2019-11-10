CLJ := $(shell find ./epiccastle.io/ -name "*.clj")
BUILD := $(patsubst %.clj,%.html,$(CLJ))
TEMPLATES := $(shell find ./epiccastle.io/templates -name "*.html")

runserver:
	cd epiccastle.io && python -m CGIHTTPServer 8000

deploy: build
	rsync -av \
		--exclude=templates \
		--exclude=*.clj \
		--exclude=*~ \
		epiccastle.io/ \
		root@epiccastle.io:/var/www/epiccastle.io/public/
	ssh root@epiccastle.io chown -R www-data:www-data /var/www/epiccastle.io
	ssh root@epiccastle.io service uwsgi restart

epiccastle.io/%.html: epiccastle.io/%.clj $(TEMPLATES)
	bootleg $< > $@

build: $(BUILD)

clean:
	-rm -f $(BUILD)

# apt-get install inotify-tools
watch:
	while true; do \
		inotifywait -r -e modify epiccastle.io ; \
		make build ; \
	done
