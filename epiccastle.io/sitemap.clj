;; find epiccastle.io -name '*.html'
(def pages
  [
   "https://epiccastle.io/crispin-wellington.html"
   "https://epiccastle.io/index.html"
   "https://epiccastle.io/blog/3/index.html"
   "https://epiccastle.io/blog/4/index.html"
   "https://epiccastle.io/blog/2/index.html"
   "https://epiccastle.io/blog/index.html"
   "https://epiccastle.io/blog/5/index.html"
   "https://epiccastle.io/blog/6/index.html"
   "https://epiccastle.io/blog/7/index.html"
   "https://epiccastle.io/blog/8/index.html"
   "https://epiccastle.io/blog/feed.xml"
   "https://epiccastle.io/blog/1/index.html"
   "https://epiccastle.io/contact.html"
   "https://epiccastle.io/spire/discussion.html"
   "https://epiccastle.io/spire/howto.html"
   "https://epiccastle.io/spire/index.html"
   "https://epiccastle.io/spire/module/stat.html"
   "https://epiccastle.io/spire/module/get-file.html"
   "https://epiccastle.io/spire/module/sysctl.html"
   "https://epiccastle.io/spire/module/authorized-keys.html"
   "https://epiccastle.io/spire/module/service.html"
   "https://epiccastle.io/spire/module/download.html"
   "https://epiccastle.io/spire/module/shell.html"
   "https://epiccastle.io/spire/module/group.html"
   "https://epiccastle.io/spire/module/curl.html"
   "https://epiccastle.io/spire/module/apt-repo.html"
   "https://epiccastle.io/spire/module/mkdir.html"
   "https://epiccastle.io/spire/module/null.html"
   "https://epiccastle.io/spire/module/apt.html"
   "https://epiccastle.io/spire/module/attrs.html"
   "https://epiccastle.io/spire/module/line-in-file.html"
   "https://epiccastle.io/spire/module/upload.html"
   "https://epiccastle.io/spire/module/pkg.html"
   "https://epiccastle.io/spire/module/apt-key.html"
   "https://epiccastle.io/spire/module/aws.html"
   "https://epiccastle.io/spire/module/rm.html"
   "https://epiccastle.io/spire/module/user.html"
   "https://epiccastle.io/spire/modules.html"
   "https://epiccastle.io/spire/tutorial.html"])

(str
 "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"
 (apply str
        (for [page pages]
          (format
           "<url><loc>%s</loc><lastmod>2021-08-19</lastmod><changefreq>monthly</changefreq><priority>1.0</priority></url>"
           page)))
 "</urlset>"
 )
