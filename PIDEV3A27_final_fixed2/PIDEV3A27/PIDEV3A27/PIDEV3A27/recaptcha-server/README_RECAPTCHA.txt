=== Google reCAPTCHA (v2 checkbox) - Desktop JavaFX (Browser externe) ===

1) Créer les clés sur Google reCAPTCHA admin:
   https://www.google.com/recaptcha/admin/create
   - Type: Challenge (v2) -> "I'm not a robot" checkbox
   - Domain: localhost (et éventuellement 127.0.0.1)

2) Mettre la SITE KEY dans:
   recaptcha-server/public/captcha.html
   Remplacer: PUT_YOUR_SITE_KEY_HERE

3) Mettre la SECRET KEY dans une variable d'environnement puis lancer le serveur:

   PowerShell:
     cd recaptcha-server
     $env:RECAPTCHA_SECRET="VOTRE_SECRET_KEY"
     npm install
     npm start

   CMD:
     cd recaptcha-server
     set RECAPTCHA_SECRET=VOTRE_SECRET_KEY
     npm install
     npm start

4) Lancer l'application JavaFX.
   Patient -> "Nouveau rendez-vous" -> Ajouter
   => Le navigateur s'ouvre pour le captcha
   => Si validé: le rendez-vous est inséré + message "Validé" + la liste est rafraîchie
   => Si échoué/annulé: aucun ajout + message "Échoué" + bouton "Réessayer"

Notes:
- Cette solution évite JavaFX WebView (qui peut crasher sur certains PC Windows).
- Le serveur Node tourne par défaut sur http://localhost:8085
