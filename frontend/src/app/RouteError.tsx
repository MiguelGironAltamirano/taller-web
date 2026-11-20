import { useRouteError } from 'react-router'
import { Button, Icon } from '../shared/ui'

/** Al publicar una versión nueva, los archivos de la anterior dejan de existir y una pantalla diferida no carga. */
const NEW_VERSION = /dynamically imported module|Importing a module script failed|error loading dynamically imported/i

/**
 * Pantalla de error de las rutas: lo que se ve si una pantalla falla al dibujarse (un error de programación) o no
 * puede cargarse. Mantiene el estilo de los estados de error y nunca muestra el error técnico; el detalle queda en
 * la consola para quien depure.
 */
export function RouteError() {
  const error = useRouteError()
  const newVersion = error instanceof Error && NEW_VERSION.test(error.message)
  const copy = newVersion
    ? {
        title: 'Hay una versión nueva de la aplicación',
        message: 'Recarga la página para seguir trabajando con la versión actualizada.',
      }
    : {
        title: 'Algo salió mal en esta pantalla',
        message: 'Tus datos están a salvo. Recarga la página; si vuelve a pasar, regresa al inicio e inténtalo otra vez.',
      }

  return (
    <div role="alert" className="flex flex-col items-center gap-2.5 px-4 py-12 text-center">
      <div className="grid size-14 place-items-center rounded-2xl bg-err-bg text-err-fg">
        <Icon name="alert" size={28} />
      </div>
      <h1 className="m-0 text-xl font-semibold">{copy.title}</h1>
      <p className="m-0 max-w-md text-muted">{copy.message}</p>
      <div className="mt-1 flex flex-wrap justify-center gap-2">
        <Button variant="primary" onClick={() => window.location.reload()}>
          Recargar la página
        </Button>
        {!newVersion && (
          <a
            href="/"
            className="inline-flex h-9 items-center rounded-lg border border-line bg-surface px-3.5 font-medium text-fg no-underline hover:border-line-strong hover:bg-surface2"
          >
            Ir al inicio
          </a>
        )}
      </div>
    </div>
  )
}
