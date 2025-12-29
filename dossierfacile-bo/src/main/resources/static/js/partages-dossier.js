// JavaScript pour la section Partages du dossier

document.addEventListener('DOMContentLoaded', function() {
    
    const accordionButtons = document.querySelectorAll('.accordion-button');
    
    accordionButtons.forEach(button => {
        button.addEventListener('click', function() {
            const content = this.nextElementSibling.nextElementSibling; // Skip separator
            const isExpanded = this.classList.contains('expanded');
            
            if (isExpanded) {
                // Fermer l'accordéon
                this.classList.remove('expanded');
                content.style.display = 'none';
                content.classList.remove('expanded');
            } else {
                // Ouvrir l'accordéon
                this.classList.add('expanded');
                content.style.display = 'block';
                content.classList.add('expanded');
            }
        });
    });

    const copyButtons = document.querySelectorAll('.btn-copy-link');
    
    copyButtons.forEach(button => {
        button.addEventListener('click', function() {
            const url = this.getAttribute('data-url');
            
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(url)
                    .then(() => {
                        showNotification('Lien copié dans le presse-papier !', 'success');
                        this.textContent = '✓ Copié';
                        setTimeout(() => {
                            this.textContent = 'Copier le lien';
                        }, 2000);
                    })
                    .catch(err => {
                        console.error('Erreur lors de la copie:', err);
                        fallbackCopyTextToClipboard(url, this);
                    });
            } else {
                // Fallback pour les navigateurs plus anciens
                fallbackCopyTextToClipboard(url, this);
            }
        });
    });
    
    function fallbackCopyTextToClipboard(text, button) {
        const textArea = document.createElement('textarea');
        textArea.value = text;
        textArea.style.position = 'fixed';
        textArea.style.left = '-999999px';
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        
        try {
            const successful = document.execCommand('copy');
            if (successful) {
                showNotification('Lien copié dans le presse-papier !', 'success');
                button.textContent = '✓ Copié';
                setTimeout(() => {
                    button.textContent = 'Copier le lien';
                }, 2000);
            } else {
                showNotification('Impossible de copier le lien', 'error');
            }
        } catch (err) {
            console.error('Erreur lors de la copie:', err);
            showNotification('Erreur lors de la copie', 'error');
        }
        
        document.body.removeChild(textArea);
    }

    // === Fonction pour afficher des notifications ===
    function showNotification(message, type = 'info') {
        // Créer l'élément de notification
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.textContent = message;
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            padding: 15px 20px;
            background-color: ${type === 'success' ? '#198754' : type === 'error' ? '#dc3545' : type === 'warning' ? '#efcb3a' : '#0d6efd'};
            color: white;
            border-radius: 4px;
            box-shadow: 0 2px 5px rgba(0,0,0,0.2);
            z-index: 9999;
            font-family: 'Marianne', sans-serif;
            font-size: 14px;
            animation: slideIn 0.3s ease-out;
        `;
        
        // Ajouter l'animation CSS
        if (!document.querySelector('#notification-styles')) {
            const style = document.createElement('style');
            style.id = 'notification-styles';
            style.textContent = `
                @keyframes slideIn {
                    from {
                        transform: translateX(100%);
                        opacity: 0;
                    }
                    to {
                        transform: translateX(0);
                        opacity: 1;
                    }
                }
                @keyframes slideOut {
                    from {
                        transform: translateX(0);
                        opacity: 1;
                    }
                    to {
                        transform: translateX(100%);
                        opacity: 0;
                    }
                }
            `;
            document.head.appendChild(style);
        }
        
        document.body.appendChild(notification);
        
        // Supprimer la notification après 3 secondes
        setTimeout(() => {
            notification.style.animation = 'slideOut 0.3s ease-out';
            setTimeout(() => {
                document.body.removeChild(notification);
            }, 300);
        }, 3000);
    }
});

