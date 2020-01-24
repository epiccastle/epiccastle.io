(function($) {
    skel.breakpoints({
        xlarge: '(max-width: 1680px)',
        large: '(max-width: 1280px)',
        medium: '(max-width: 980px)',
        small: '(max-width: 736px)',
        xsmall: '(max-width: 480px)'
    });

    $(function() {
        var $window = $(window), $body = $('body');

        // Touch mode.
        if (skel.vars.mobile)
            $body.addClass('is-touch');

        // Fix: Placeholder polyfill.
        $('form').placeholder();

        // Prioritize "important" elements on medium.
        skel.on('+medium -medium', function() {
            $.prioritize(
                '.important\\28 medium\\29',
                skel.breakpoint('medium').active
            );
        });

        // Scrolly links.
        $('.scrolly').scrolly({
            speed: 2000
        });

        // Dropdowns.
        $('#nav > ul').dropotron({
            alignment: 'right',
            hideDelay: 350
        });

        var change_banner_title = function() {
            pos = document.body.scrollTop || document.documentElement.scrollTop;
            heading = $(".title-heading");
            heading_text = heading.text()
            if(heading_text && pos>(heading.offset().top - 48))
            {
                $("#logo-heading").text(heading_text);
                $("#logo-heading-small").text(heading_text);
            }
            else
            {
                $("#logo-heading").text("Epic Castle");
                $("#logo-heading-small").text("Epic Castle");
            }
        };

        // on load set the title correctly
        change_banner_title();

        // changing title
        $window.scroll(change_banner_title);

        // Off-Canvas Navigation.

        // Title Bar.
        $(
            '<div id="titleBar" style="overflow: hidden;">' +
                '<header>'+
                '<a href="/" style="border-bottom: none;"><img src="/images/logo.png" alt="" style="width: 1.5em; margin: 0.125em 0.25em 0 1em;"/></a>' +
                '<h1 id="logo-heading-small" style="top: 0.4em; line-height: 2em;">' +
                'Epic Castle' +
                '</h1>' +
                '<a href="#navPanel" style="border-bottom: none; float: right; padding-right: 16px; padding-top: 7px; padding-left: 16px; padding-bottom: 7px;"><img src="/images/fa-bars.png" style="width: 16px; opacity: 0.5;"/></a>' +
                '</header>' +
                '</div>'
        ).appendTo($body);

        // Navigation Panel.
        $(
            '<div id="navPanel">' +
                '<nav>' +
                $('#nav').navList() +
                '</nav>' +
                '</div>'
        )
            .appendTo($body)
            .panel({
                delay: 500,
                hideOnClick: true,
                hideOnSwipe: true,
                resetScroll: true,
                resetForms: true,
                side: 'right',
                target: $body,
                visibleClass: 'navPanel-visible'
            });

        // Fix: Remove navPanel transitions on WP<10 (poor/buggy performance).
        if (skel.vars.os == 'wp' && skel.vars.osVersion < 10)
            $('#titleBar, #navPanel, #page-wrapper').css('transition', 'none');

        // Parallax.
        // Disabled on IE (choppy scrolling) and mobile platforms (poor performance).
        if (skel.vars.browser == 'ie'
            ||	skel.vars.mobile) {

            $.fn._parallax = function() {
                return $(this);
            };

        }
        else {
            $.fn._parallax = function() {
                $(this).each(function() {
                    var $this = $(this),
                        on, off;
                    on = function() {
                        $this.css('background-position', 'center 0px');
                        $window.on('scroll._parallax', function() {
                            var pos = parseInt($window.scrollTop()) - parseInt($this.position().top);
                            $this.css('background-position', 'center ' + (pos * -0.15) + 'px');
                        });
                    };

                    off = function() {

                        $this
                            .css('background-position', '');

                        $window
                            .off('scroll._parallax');

                    };

                    skel.on('change', function() {

                        if (skel.breakpoint('medium').active)
                            (off)();
                        else
                            (on)();

                    });

                });

                return $(this);

            };

            $window
                .on('load resize', function() {
                    $window.trigger('scroll');
                });

        }

        // Spotlights.
        var $spotlights = $('.spotlight');

        $spotlights
            ._parallax()
            .each(function() {

                var $this = $(this),
                    on, off;

                on = function() {

                    // Use main <img>'s src as this spotlight's background.
                    $this.css('background-image', 'url("' + $this.find('.image.main > img').attr('src') + '")');

                };

                off = function() {

                    // Clear spotlight's background.
                    $this.css('background-image', '');


                };

                skel.on('change', function() {

                    if (skel.breakpoint('medium').active)
                        (off)();
                    else
                        (on)();

                });

            });

        // Banner.
        var $banner = $('#banner');
        $banner._parallax();

        // Carousel
        if($('.crsl-items').length)
            $('.crsl-items').carousel({
                speed: 1000,
                autoRotate: 6000,
                visible: 5,
                itemMinWidth: 100,
                itemMargin: 70});

        var message_send_failed = function() {
            $("h3#result").html("Message Send Failed!").css("color", "red");
            $("#mask-overlay").css('pointer-events','inherit');
            $("div#contact-us-form").css('pointer-events','none');
        };

        var message_send_success = function() {
            $("h4#result-sub").html("");
        };

        var send_message = function(name, email, message) {
            return $.post('/cgi-bin/contact.py',
                          {
                              'name': name,
                              'email': email,
                              'message': message
                          });
        };

        window.contact_us = function () {
            $("input#contact-submit").prop('disabled', true);
            var name = $('input#name').val();
            var email = $('input#email').val();
            var message = $('textarea#message').val();

            if(!email.length || !name.length || !message.length)
            {
                if(!email.length) $('input#email').addClass('shake');
                if(!name.length) $('input#name').addClass('shake');
                if(!message.length) $('textarea#message').addClass('shake');
                setTimeout(function() {
                    $('input#email').removeClass('shake');
                    $('input#name').removeClass('shake');
                    $('textarea#message').removeClass('shake');
                    $("input#contact-submit").prop('disabled', false);
                },
                820
                );
                return;
            }
            else{
                $("input#contact-submit").prop('value', "Sending...");

                send_message(name, email, message)
                    .always(function(){$("#mask-overlay").css('opacity', 1);
                                       $("div#contact-us-form").css('opacity', 0);})
                    .done(message_send_success)
                    .fail(message_send_failed);
            }
        }
    });
})(jQuery);
