# 🎯 Guida per Generare QR Code per i Rifugi

## 📱 **Opzione 1: Siti Web Online (Raccomandato)**

### **QR Code Generator**
- **URL**: https://www.qr-code-generator.com/
- **Passi**:
  1. Vai sul sito
  2. Inserisci il contenuto: `rifugio_1` (per rifugio ID 1)
  3. Clicca "Generate QR Code"
  4. Scarica l'immagine PNG

### **QR Code Monkey**
- **URL**: https://www.qrcode-monkey.com/
- **Vantaggi**: Personalizzabile con colori e logo

### **Google Charts API**
- **URL**: `https://chart.googleapis.com/chart?chs=300x300&cht=qr&chl=rifugio_1&choe=UTF-8`
- Sostituisci `rifugio_1` con l'ID del rifugio

## 📋 **Formato QR Code**
```
rifugio_1    → Rifugio con ID 1
rifugio_2    → Rifugio con ID 2
rifugio_3    → Rifugio con ID 3
...
rifugio_10   → Rifugio con ID 10
```

## 🖨️ **Opzione 2: App Mobile**

### **QR Code Generator (Android)**
- Scarica da Google Play Store
- Inserisci il testo: `rifugio_X`
- Salva l'immagine

### **QR & Barcode Scanner (iOS)**
- App nativa per generare QR code
- Inserisci il contenuto e salva

## 💻 **Opzione 3: Script Python (Per Sviluppatori)**

```python
import qrcode

def generate_qr_for_rifugio(rifugio_id):
    qr = qrcode.QRCode(version=1, box_size=10, border=5)
    qr.add_data(f"rifugio_{rifugio_id}")
    qr.make(fit=True)
    
    img = qr.make_image(fill_color="black", back_color="white")
    img.save(f"qr_rifugio_{rifugio_id}.png")

# Genera QR per i primi 10 rifugi
for i in range(1, 11):
    generate_qr_for_rifugio(i)
```

## 🎯 **Come Testare**

1. **Genera QR code** con uno dei metodi sopra
2. **Salva l'immagine** sul telefono
3. **Apri l'app** Mountain Passport
4. **Vai alla sezione Scan**
5. **Scansiona il QR code**
6. **Verifica** che la visita sia registrata

## 📝 **Note Importanti**

- **Formato**: Deve essere esattamente `rifugio_X` dove X è l'ID
- **Dimensione**: Consigliato 300x300 pixel o più
- **Qualità**: Usa formato PNG per migliore qualità
- **Test**: Sempre testare prima di stampare/distribuire

## 🖨️ **Stampa QR Code**

- **Dimensioni**: Minimo 2x2 cm per scansione facile
- **Materiale**: Carta resistente o plastica
- **Posizionamento**: All'ingresso del rifugio, ben visibile
- **Backup**: Avere sempre un QR code di riserva
