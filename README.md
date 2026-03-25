# GymApp

Aplicativo Android para organizar treinos de academia por semanas e dias, com registro de exercícios, séries e cargas, sincronização em nuvem e acompanhamento de evolução.

---

## Funcionalidades

- **Semanas de treino** — crie, renomeie e apague semanas; visualize quantos treinos foram concluídos
- **Treinos por semana** — adicione treinos a cada semana, marque como concluído, copie para outra semana
- **Exercícios** — biblioteca de exercícios por categoria com confirmação antes de apagar
- **Séries** — registre peso e repetições por exercício; visualize em tabela com delete individual
- **Histórico de evolução** — veja maior carga, última carga e histórico completo por sessão de cada exercício
- **Cronômetro** — timer com notificação persistente na barra de status com botões de pausar/continuar/parar
- **Tema escuro** — segue automaticamente o tema do sistema
- **Sincronização Firebase** — dados salvos e sincronizados na nuvem em tempo real

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Kotlin |
| UI | Jetpack Compose + Material Design 3 |
| Arquitetura | MVVM + Repository Pattern |
| Navegação | Navigation Compose |
| Estado | StateFlow + ViewModel |
| Backend | Firebase Firestore + Firebase Auth |
| Autenticação | Firebase Anonymous Auth |
| Serialização | Gson |
| Async | Kotlin Coroutines |
| Notificações | Android Notification API + BroadcastReceiver |
| Build | Gradle (Kotlin DSL) |

---

## 🚀 Como configurar e executar

### Pré-requisitos

- Android Studio Hedgehog (2023.1.1) ou superior
- JDK 17+
- Conta no [Firebase Console](https://console.firebase.google.com)

### 1. Clone o repositório

```bash
git clone https://github.com/robbribeiro/gymapp.git
cd gymapp
```

### 2. Configure o Firebase

O arquivo `google-services.json` **não está incluído** no repositório por conter chaves sensíveis. Você precisa criar o seu:

1. Acesse [console.firebase.google.com](https://console.firebase.google.com)
2. Crie um novo projeto (ou use um existente)
3. Adicione um app Android com o package name: `com.gymapp`
4. Baixe o arquivo `google-services.json` gerado
5. Coloque o arquivo em `app/google-services.json`

### 3. Configure o Firebase Authentication

No Firebase Console:
1. Acesse **Authentication → Começar**
2. Na aba **Sign-in method**, habilite **Anônimo**

### 4. Configure o Firestore

No Firebase Console:
1. Acesse **Firestore Database → Criar banco de dados**
2. Selecione **Modo de produção**
3. Escolha a região (recomendado: `southamerica-east1`)
4. Configure as regras de segurança (veja seção abaixo)

### 5. Regras de segurança do Firestore

No Firebase Console, em **Firestore → Regras**, configure:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### 6. Execute o projeto

1. Abra o projeto no Android Studio
2. Aguarde a sincronização do Gradle
3. Crie ou selecione um emulador (API 24+) em **Tools → Device Manager**
4. Execute com **Shift + F10** ou o botão ▶

---

## 🗂️ Estrutura do projeto

```
app/src/main/java/com/gymapp/
├── data/
│   └── firebase/
│       ├── FirebaseRepositoryOptimized.kt   # Acesso ao Firestore
│       └── [modelos de dados]               # Exercise, Workout, Set, WorkoutWeek...
├── service/
│   ├── TimerNotificationService.kt          # Notificação do cronômetro
│   └── TimerNotificationReceiver.kt         # BroadcastReceiver dos botões
├── ui/
│   ├── components/                          # Componentes reutilizáveis
│   │   ├── WeekCard.kt
│   │   ├── WorkoutCard.kt
│   │   └── ExerciseCard.kt
│   ├── navigation/
│   │   └── GymAppNavigation.kt              # Grafo de navegação
│   ├── screens/
│   │   ├── workouts/                        # Telas de treinos e semanas
│   │   ├── exercises/                       # Biblioteca + histórico de exercícios
│   │   └── timer/                           # Cronômetro
│   ├── theme/
│   │   ├── Theme.kt                         # Light + Dark theme
│   │   └── Type.kt
│   └── viewmodel/
│       └── UnifiedWorkoutViewModel.kt       # ViewModel principal
├── utils/
│   ├── LogTags.kt
│   └── PermissionUtils.kt
└── MainActivity.kt
```

---

## 🔒 Segurança

- O arquivo `google-services.json` está no `.gitignore` e **nunca deve ser commitado**
- A autenticação é anônima — cada instalação recebe um UID único automaticamente
- As regras do Firestore garantem que cada usuário acessa apenas seus próprios dados
- Não há senhas ou dados pessoais armazenados

---

## 📋 Requisitos do sistema

| Item | Versão mínima |
|---|---|
| Android | 7.0 (API 24) |
| Android Studio | Hedgehog 2023.1.1 |
| Gradle | 8.13 |
| Kotlin | 1.9.22 |

---

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para mais detalhes.
