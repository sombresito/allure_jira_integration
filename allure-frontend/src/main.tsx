import ReactDOM from 'react-dom/client';
import { CssBaseline, createTheme, ThemeProvider } from '@mui/material';
import { createBrowserRouter, RouterProvider } from 'react-router-dom';

import '@fontsource/roboto/cyrillic-400.css';

import AppLayout from './layouts/AppLayout';
import Home from './pages/Home';
import ReportsMain  from './pages/ReportsMain';
import ReportsPreprod from './pages/ReportsPreprod';
import ReportsTest from './pages/ReportsTest';
import ReportsDev from './pages/ReportsDev';
import ReportsETE from './pages/ReportsETE';
import ReportsColvir from './pages/ReportsColvir';
import ReportsFull from './pages/ReportsFull';

const router = createBrowserRouter(
  [{
    path: '/',
    element: <AppLayout/>,
    children: [
      { index: true,         element: <Home/>           },
      { path: 'load',        element: <Home name='Load'/> },
      { path: 'synthetic',   element: <Home name='Synthetic'/> },
      { path: 'mock',        element: <Home name='Mock'/> },
    ],
  },
  { path: '/reports-main', element: <ReportsMain /> },
  { path: '/reports-preprod', element: <ReportsPreprod /> },
  { path: '/reports-test', element: <ReportsTest /> },
  { path: '/reports-dev', element: <ReportsDev /> },
  { path: '/reports-ete', element: <ReportsETE /> },
  { path: '/reports-colvir', element: <ReportsColvir /> },
  { path: '/reports-full', element: <ReportsFull /> },
  ],
  { basename: '/ui' }
);

ReactDOM.createRoot(document.getElementById('root')!)
        .render(
          <ThemeProvider theme={createTheme()}>
            <CssBaseline/>
            <RouterProvider router={router}/>
          </ThemeProvider>);




