# sv

Everything you need to build a Svelte project, powered by [`sv`](https://github.com/sveltejs/cli).

## Configuration

### API Base URL

The frontend connects to a backend API. The API URL is configured via environment variables.

1. Copy `.env.example` to `.env`:
   ```sh
   cp .env.example .env
   ```

2. Edit `.env` to set your API base URL:
   ```env
   VITE_API_BASE_URL=http://localhost:8080
   ```

The default value is `http://localhost:8080` if not specified.

## Creating a project

If you're seeing this, you've probably already done this step. Congrats!

```sh
# create a new project in the current directory
npx sv create

# create a new project in my-app
npx sv create my-app
```

## Developing

Once you've created a project and installed dependencies with `npm install` (or `pnpm install` or `yarn`), start a development server:

```sh
npm run dev

# or start the server and open the app in a new browser tab
npm run dev -- --open
```

## Building

To create a production version of your app:

```sh
npm run build
```

You can preview the production build with `npm run preview`.

> To deploy your app, you may need to install an [adapter](https://svelte.dev/docs/kit/adapters) for your target environment.
