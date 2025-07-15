/* -----------------------------------------------------------------------
 *  ReportsMain.tsx
 *  Страница-оглавление «Reports bcc hub» на React + MUI + bcc-design
 * --------------------------------------------------------------------- */

import {
  AppBar,
  Box,
  Drawer,
  List,
  ListItemButton,
  ListItemText,
  Toolbar,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';

import {
  Button as BccButton,
  EButtonVariant,
  Logotype,
} from 'bcc-design';

import { useEffect, useState } from 'react';

/* === константы ======================================================= */

const DRAWER_WIDTH = 300;

/* ------------------------------ ссылки ------------------------------ */
const VAADIN_LINKS = [

   { href: '/ui/reports-preprod', label: 'Отчёты Preprod окружения' },
   { href: '/ui/reports-test', label: 'Отчёты Test окружения' },
   { href: '/ui/reports-dev', label: 'Отчёты Dev окружения' },
   { href: '/ui/vn/reports-ete', label: 'Отчёты E2E тестирования' },
   { href: '/ui/vn/reports-colvir', label: 'Отчёты Colvir' },
   { href: '/ui/vn/reports-view', label: 'Хранилище отчетов' },
   { href: '/ui/vn/results',         label: 'Результаты'      },
   { href: '/ui/vn/about',           label: 'О приложении'    },


];

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const SWAGGER_LINK = { href: '/ui/vn/swagger', label: 'Swagger' };
/* -------------------------------------------------------------------- */

export default function ReportsMain() {
  const theme    = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));

  const [isAdmin, setIsAdmin] = useState(false);

  /* пример проверки роли (замените на своё API) */
  useEffect(() => {
    fetch('/api/user/info')
      .then(r => (r.ok ? r.json() : null))
      .then(p => setIsAdmin(p?.roles?.includes('ROLE_ADMIN') ?? false))
      .catch(() => { /* игнорируем */ });
  }, []);

  const links = isAdmin ? [...VAADIN_LINKS, SWAGGER_LINK] : VAADIN_LINKS;

  /* === рендер ======================================================= */

  return (
    <Box sx={{ display: 'flex' }}>

      {/* ─────────── Header ─────────── */}
      <AppBar
        position="fixed"
        sx={{
          zIndex: (t) => t.zIndex.drawer + 1,
          backgroundColor: '#2dba8d',
          color: '#000',
        }}
      >
        <Toolbar sx={{ minHeight: 56, px: 3 }}>

          {/* логотип + текст */}
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography
              component="span"
              sx={{
                fontFamily: '"Inter", sans-serif',
                fontSize: '1.25rem',
                fontWeight: 600,
                lineHeight: 1,
                transform: 'translateY(-2px)',
              }}
            >
              Reports
            </Typography>

            <Box sx={{ display: 'inline-block', transform: 'translateY(2px)' }}>
              <Logotype.BccHub size="sm" />
            </Box>
          </Box>

          <Box sx={{ flexGrow: 1 }} />

          {/* Кнопка «Вернуться на главную» */}
          <Box
              sx={{
                px: isMobile ? 2 : 3,
                py: isMobile ? 0.75 : 1,
                backgroundColor: '#E6E7EB',
                color: '#2E3034',
                borderRadius: 2,
                fontWeight: 600,
                display: 'inline-block',
                '&:hover': { backgroundColor: '#DADBDF' },
              }}
            >
              <BccButton
                view={EButtonVariant.NeutralFilledSecondary}
                size={isMobile ? 'm' : 'l'}
                onClick={() => (window.location.href = '/ui/')}
                style={{       /* кнопку делаем «прозрачной» */
                  background: 'transparent',
                  border: 0,
                  padding: 0,
                  minWidth: 0,
                  color: 'inherit',
                  font: 'inherit',
                }}
              >
                Вернуться&nbsp;на&nbsp;главную
              </BccButton>
            </Box>

        </Toolbar>
      </AppBar>

      {/* ─────────── Drawer ─────────── */}
      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          [`& .MuiDrawer-paper`]: {
            width: DRAWER_WIDTH,
            boxSizing: 'border-box',
            top: 56,      // вертикальный отступ = высота AppBar
            pt : 2,       // «запас воздуха» под шапкой
          },
        }}
      >
        <List disablePadding>
          {links.map(({ href, label }) => {
            const active = window.location.pathname === href;
            return (
              <ListItemButton
                key={href}
                component="a"
                href={href}
                selected={active}
                sx={{
                  whiteSpace: 'nowrap',
                  overflow:   'hidden',
                  textOverflow: 'ellipsis',
                  '&.Mui-selected': {
                    bgcolor: 'rgba(39,174,96,0.12)',
                  },
                  '&:hover': {
                    bgcolor: 'rgba(39,174,96,0.08)',
                    '& .MuiListItemText-primary': {
                      color: 'rgb(39,174,96)',
                    },
                  },
                }}
              >
                <ListItemText primary={label} primaryTypographyProps={{ noWrap: true }} />
              </ListItemButton>
            );
          })}
        </List>
      </Drawer>

      {/* ─────────── Main ─────────── */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 4,
          mt: '56px',               // отодвигаем под AppBar
          display: 'flex',
          justifyContent: 'center', // центрируем карточку
        }}
      >
        <Box
          sx={{
            maxWidth: 860,
            width: '100%',
            p: 3,
            mt: 25,
            borderRadius: 2,
            backgroundColor: t => t.palette.grey[100],
            border: '1px solid',
            borderColor: t => t.palette.grey[300],
          }}
        >
          <Typography component="p" mb={2}>
            <b>На&nbsp;странице отчётов:</b>&nbsp;предоставляются подробные
            отчёты по&nbsp;результатам автоматизированных и&nbsp;мануальных тестов.
            Пользователь может анализировать выполненные тесты, их&nbsp;успешность,
            длительность и&nbsp;обнаруженные ошибки.
          </Typography>


        </Box>
      </Box>
    </Box>
  );
}
