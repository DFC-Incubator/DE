(function(){function n(e){return e.replace(/<[^>]*>?/gi,o)}function o(e){if(e.match(r)||e.match(i)||e.match(s))return e;else return""}function u(e){if(e=="")return"";var t=/<\/?\w+[^>]*(\s|$|>)/g;var n=e.toLowerCase().match(t);var r=(n||[]).length;if(r==0)return e;var i,s;var o="<p><img><br><li><hr>";var u;var a=[];var f=[];var l=false;for(var c=0;c<r;c++){i=n[c].replace(/<\/?(\w+).*/,"$1");if(a[c]||o.search("<"+i+">")>-1)continue;s=n[c];u=-1;if(!/^<\//.test(s)){for(var h=c+1;h<r;h++){if(!a[h]&&n[h]=="</"+i+">"){u=h;break}}}if(u==-1)l=f[c]=true;else a[u]=true}if(!l)return e;var c=0;e=e.replace(t,function(e){var t=f[c]?"":e;c++;return t});return e}var e,t;if(typeof exports==="object"&&typeof require==="function"){e=exports;t=require("./Markdown.Converter").Converter}else{e=window.Markdown;t=e.Converter}e.getSanitizingConverter=function(){var e=new t;e.hooks.chain("postConversion",n);e.hooks.chain("postConversion",u);return e};var r=/^(<\/?(b|blockquote|code|del|dd|dl|dt|em|h1|h2|h3|i|kbd|li|ol|p|pre|s|sup|sub|strong|strike|ul)>|<(br|hr)\s?\/?>)$/i;var i=/^(<a\shref="((https?|ftp):\/\/|\/)[-A-Za-z0-9+&@#\/%?=~_|!:,.;\(\)]+"(\stitle="[^"<>]+")?\s?>|<\/a>)$/i;var s=/^(<img\ssrc="(https?:\/\/|\/)[-A-Za-z0-9+&@#\/%?=~_|!:,.;\(\)]+"(\swidth="\d{1,3}")?(\sheight="\d{1,3}")?(\salt="[^"<>]*")?(\stitle="[^"<>]*")?\s?\/?>)$/i})()