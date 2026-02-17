#!/bin/bash

echo "========================================"
echo "   Lancement de MindCare Application"
echo "========================================"
echo ""

echo "Vérification de Maven..."
if ! command -v mvn &> /dev/null; then
    echo "ERREUR: Maven n'est pas installé ou n'est pas dans le PATH"
    echo "Veuillez installer Maven depuis https://maven.apache.org/"
    exit 1
fi

echo "Maven trouvé!"
echo ""

echo "Compilation du projet..."
mvn clean compile
if [ $? -ne 0 ]; then
    echo "ERREUR: La compilation a échoué"
    exit 1
fi

echo ""
echo "Compilation réussie!"
echo ""

echo "Lancement de l'application JavaFX..."
mvn javafx:run
