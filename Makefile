CLJ := $(shell find ./epiccastle.io/ -name "*.clj")
SPLASH := $(shell find ./epiccastle.io/ -name "splash.png")
BUILD := $(patsubst %.clj,%.html,$(CLJ))
SPLASH_BUILD := $(patsubst %.png,%-20.png,$(SPLASH))
TEMPLATES := $(shell find ./epiccastle.io/templates -name "*.html")
BOOTLEG := bootleg
#BOOTLEG := java -jar ../bootleg/target/uberjar/bootleg-0.1.7-SNAPSHOT-standalone.jar

runserver:
	cd epiccastle.io && python -m CGIHTTPServer 8000

deploy: clean build
	rsync -av \
		--exclude=templates \
		--exclude=*.clj \
		--exclude=*~ \
		epiccastle.io/ \
		root@epiccastle.io:/var/www/epiccastle.io/public/
	ssh root@epiccastle.io chown -R www-data:www-data /var/www/epiccastle.io
	ssh root@epiccastle.io service uwsgi restart

epiccastle.io/%.html: epiccastle.io/%.clj $(TEMPLATES)
	$(BOOTLEG) -o $@ $<

build: $(BUILD) $(SPLASH_BUILD)

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

epiccastle.io/blog/%/splash-20.png: epiccastle.io/blog/%/splash.png
	convert $< -resize 1000000@ $@

epiccastle.io/images/fa-%.png: svg/%.png
	cp $< $@

fa-thumbs: epiccastle.io/images/fa-code-branch.png epiccastle.io/images/fa-cogs.png epiccastle.io/images/fa-paper-plane.png epiccastle.io/images/fa-credit-card.png epiccastle.io/images/fa-cloud.png epiccastle.io/images/fa-bell.png epiccastle.io/images/fa-angle-left.png epiccastle.io/images/fa-angle-right.png epiccastle.io/images/fa-bars.png
