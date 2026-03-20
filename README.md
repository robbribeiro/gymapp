# GymApp - Aplicativo de Academia 🏋️‍♂️

Um aplicativo Android moderno para monitorar sua progressão de carga, repetições e exercícios na academia, com sincronização em nuvem via Firebase.

## 📱 Funcionalidades

### ✅ Implementadas
- **Gerenciamento de Treinos**: Criar, visualizar e executar treinos organizados por semanas
- **Biblioteca de Exercícios**: Catálogo completo com exercícios pré-definidos
- **Cronômetro de Descanso**: Timer visual circular para intervalos entre séries
- **Execução de Treinos**: Interface para registrar séries, peso e repetições em tempo real
- **Sincronização Firebase**: Backup automático e sincronização de dados na nuvem
- **Interface Moderna**: Design Material Design 3 com Jetpack Compose
- **Navegação Intuitiva**: Bottom navigation com 3 seções principais

### 🎯 Principais Recursos
- **Organização por Semanas**: Treinos agrupados por semanas para melhor controle
- **Timer Circular**: Cronômetro visual com progresso em tempo real
- **Sincronização Automática**: Dados salvos automaticamente no Firebase
- **Interface Responsiva**: Design adaptável para diferentes tamanhos de tela
- **Notificações**: Sistema de notificações para o cronômetro
- **Persistência Híbrida**: Cache local + sincronização em nuvem

## 🛠️ Tecnologias Utilizadas

- **Linguagem**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **Arquitetura**: MVVM com Repository Pattern
- **Backend**: Firebase (Firestore, Auth, Storage, Analytics)
- **Navegação**: Navigation Compose
- **Gerenciamento de Estado**: Compose State + ViewModel
- **Serialização**: Gson
- **Coroutines**: Para operações assíncronas
- **Notificações**: Sistema nativo do Android

## 📦 Dependências Principais

```kotlin
// UI e Compose
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.activity:activity-compose")
implementation("androidx.navigation:navigation-compose")

// ViewModel e Lifecycle
implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
implementation("androidx.lifecycle:lifecycle-runtime-ktx")

// Firebase BOM (gerencia versões)
implementation(platform("com.google.firebase:firebase-bom:32.7.0"))

// Firebase Services
implementation("com.google.firebase:firebase-firestore-ktx")
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-storage-ktx")
implementation("com.google.firebase:firebase-analytics-ktx")

// Coroutines para Firebase
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services")

// Outros
implementation("com.google.code.gson:gson")
implementation("androidx.compose.material:material-icons-extended")
```

## 🚀 Como Executar

1. **Clone o repositório**
2. **Abra no Android Studio** (versão Arctic Fox ou superior)
3. **Configure o Firebase**:
   - O arquivo `google-services.json` já está incluído
   - O projeto está configurado para usar Firebase automaticamente
4. **Execute o projeto** no emulador ou dispositivo físico

### Requisitos
- Android Studio Arctic Fox ou superior
- SDK mínimo: API 24 (Android 7.0)
- SDK alvo: API 34 (Android 14)
- Conexão com internet (para sincronização Firebase)

## 📱 Telas do App

### 💪 Treinos
- **Lista de Semanas**: Visualização de treinos organizados por semanas
- **Gerenciamento de Semanas**: Criar, editar e excluir semanas de treino
- **Detalhes do Treino**: Visualizar exercícios e séries de cada treino
- **Execução de Treinos**: Interface para registrar séries em tempo real

### 📚 Exercícios
- **Biblioteca Completa**: Catálogo com exercícios pré-definidos
- **Busca e Filtros**: Encontrar exercícios por nome ou categoria
- **Histórico de Cargas**: Visualizar progressão de peso e repetições

### ⏱️ Cronômetro
- **Timer Circular**: Interface visual com progresso em tempo real
- **Controles Intuitivos**: Play, pause e reset com botões grandes
- **Notificações**: Alertas quando o tempo de descanso termina
- **Design Minimalista**: Foco total no cronômetro durante o treino

## 🎨 Design

O app utiliza Material Design 3 com:
- Cores dinâmicas adaptáveis ao sistema
- Componentes modernos (Cards, FABs, Navigation Bar)
- Tipografia clara e hierárquica
- Ícones intuitivos do Material Icons

## 🔧 Estrutura do Projeto

```
app/src/main/java/com/gymapp/
├── data/
│   ├── firebase/         # Integração com Firebase
│   │   ├── FirebaseConfig.kt
│   │   ├── FirebaseRepositoryOptimized.kt
│   │   └── FirebaseCache.kt
│   ├── persistence/      # Cache local
│   │   ├── LocalCache.kt
│   │   └── WorkoutPersistence.kt
│   └── repository/       # Repository pattern
│       ├── HybridRepository.kt
│       └── OptimizedRepository.kt
├── ui/
│   ├── components/       # Componentes reutilizáveis
│   │   ├── WorkoutCard.kt
│   │   ├── ExerciseCard.kt
│   │   ├── RestTimer.kt
│   │   └── WeekCard.kt
│   ├── navigation/        # Navegação do app
│   │   └── GymAppNavigation.kt
│   ├── screens/          # Telas principais
│   │   ├── workouts/     # Telas de treinos
│   │   ├── exercises/    # Tela de exercícios
│   │   └── timer/        # Tela do cronômetro
│   ├── theme/            # Tema e cores
│   │   ├── Theme.kt
│   │   └── Type.kt
│   └── viewmodel/        # ViewModels
│       ├── UnifiedWorkoutViewModel.kt
│       ├── TimerViewModel.kt
│       └── WorkoutWeek.kt
├── service/              # Serviços em background
│   ├── TimerNotificationService.kt
│   └── TimerNotificationReceiver.kt
├── utils/                 # Utilitários
│   ├── LogTags.kt
│   └── PermissionUtils.kt
└── MainActivity.kt        # Activity principal
```
## 🔥 Firebase Integration

O app utiliza Firebase para:
- **Firestore**: Armazenamento de dados de treinos e exercícios
- **Authentication**: Autenticação anônima para identificação única
- **Storage**: Backup de dados importantes
- **Analytics**: Métricas de uso do aplicativo

### Configuração Automática
- O projeto já está configurado com Firebase
- Arquivo `google-services.json` incluído
- Sincronização automática habilitada por padrão
