CLJ := $(shell find ./epiccastle.io/ -name "*.clj" ! -name "lib.clj" ! -name ".#*")
SPLASH_PNG := $(shell find ./epiccastle.io/ -name "splash.png")
SPLASH_JPG := $(shell find ./epiccastle.io/ -name "splash.jpg")
BUILD := $(patsubst %.clj,%.html,$(CLJ))
SPLASH_PNG_BUILD := $(patsubst %.png,%-20.png,$(SPLASH_PNG))
SPLASH_JPG_BUILD := $(patsubst %.jpg,%-20.jpg,$(SPLASH_JPG))
TEMPLATES := $(shell find ./epiccastle.io/templates -name "*.html")
BB := bb

runserver:
	cd epiccastle.io && python -m http.server --cgi 8000

deploy: clean build
	rsync -av \
		--exclude=templates \
		--exclude=*.clj \
		--exclude=*~ \
		epiccastle.io/ \
		root@epiccastle.io:/var/www/epiccastle.io/public/
	ssh root@epiccastle.io chown -R www-data:www-data /var/www/epiccastle.io
	ssh root@epiccastle.io service uwsgi restart

epiccastle.io/%.html: epiccastle.io/%.clj epiccastle.io/lib.clj $(TEMPLATES)
	cd $(dir $<) && $(BB) $(notdir $<) > $(notdir $@)

build: $(BUILD) $(SPLASH_PNG_BUILD) $(SPLASH_JPG_BUILD)
	mv epiccastle.io/sitemap.html epiccastle.io/sitemap.xml

clean:
	-rm -f $(BUILD)

# apt-get install inotify-tools
watch:
	while true; do \
		inotifywait -r -e modify epiccastle.io ; \
		make build ; \
	done

spire-analysis:
	rm spire-analysis.edn
	-clj-kondo --lint ../spire/src/ --config '{:output {:analysis true :format :edn}}' > spire-analysis.edn

epiccastle.io/blog/%/splash-20.jpg: epiccastle.io/blog/%/splash.jpg
	convert $< -resize 1000000@ $@
