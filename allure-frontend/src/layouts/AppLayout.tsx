// src/layouts/AppLayout.tsx
import {
  AppBar,
  Box,
  Drawer,
  List,
  ListItemButton,
  ListItemText,
  Toolbar,
} from '@mui/material';
import { NavLink, Outlet } from 'react-router-dom';

// Компоненты из bcc-design
import {
  Logotype
} from 'bcc-design';

const drawerWidth = 300;

// Список ссылок: React, Vaadin и внешние
/* ── меню ─────────────────────── */
 const links = [
   /* React */

   /* Vaadin */
   { to: 'reports-main', label: 'Отчёты автотестов' },
   //{ to: 'vn/reports-main', label: 'Отчёты автотестов', vaadin: true },
   { to: 'vn/load-main',      label: 'Нагрузочное тестирование', vaadin: true },
   { to: 'vn/Gen-Syn',         label: 'Генератор синтетики',      vaadin: true },
   { to: 'vn/mock-main',       label: 'Mock Server',              vaadin: true },


   /* Внешний ресурс */
   {
     to: 'http://10.15.123.137:9090/',
     label: 'Помощник NoMadAI',
     external: true,
   },
 ];

export default function AppLayout() {
  return (
    <Box sx={{ display: 'flex' }}>
      {/* ─── Header ─── */}
      <AppBar
        position="fixed"
        //color="success"
        sx={{
            zIndex: theme => theme.zIndex.drawer + 1,
            backgroundColor: '#2dba8d',  // ваш кастомный цвет
            color: '#000',
          }}
      >
        <Toolbar>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              {/* Текст чуть поднимаем */}
              <Box
                component="span"
                sx={{
                  fontFamily: '"Inter", sans-serif',
                  fontSize: '1.25rem',
                  fontWeight: 600,
                  lineHeight: 1,
                  // подбираете значение в px, чтобы текст оказался ровно на уровне иконки
                  transform: 'translateY(-2px)',
                }}
              >
                Allure Server
              </Box>
              {/* Или вместо текста — двигаем иконку */}
              <Box
                component="span"
                sx={{
                  display: 'inline-block',
                  // подбираете в px (или %), чтобы icon выровнялся
                  transform: 'translateY(2px)',
                }}
              >
                <Logotype.BccHub size="sm" />
              </Box>
            </Box>

        </Toolbar>

      </AppBar>

      {/* ─── Side Drawer ─── */}
      <Drawer
        variant="permanent"
        sx={{
          width: drawerWidth,
          flexShrink: 0,
          '& .MuiDrawer-paper': {
            width: drawerWidth,
            boxSizing: 'border-box',
            top: 64, // высота AppBar
            p: 0,
          },
        }}
      >
        <List>
          {links.map(({ to, label, external, vaadin }) => (
              <ListItemButton
                key={to}
                component={external || vaadin ? 'a' : NavLink}
                href={external || vaadin ? to : undefined}
                to={external || vaadin ? undefined : to}
                {...(vaadin && { reloadDocument: true })}
                sx={{
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis',
                  '&.active': {
                    backgroundColor: '#e0e0e0',
                  },
                  // вот сюда ваши hover-стили
                  '&:hover': {
                    backgroundColor: 'rgba(39,174,96,0.1)',        // нежно-зелёный фон
                    color: 'rgb(39,174,96)',                       // зелёный цвет текста
                    // если у вас текст в .MuiListItemText и не наследует напрямую:
                    '& .MuiListItemText-primary': {
                      color: 'rgb(39,174,96)',
                    },
                  },
                }}
              >
                <ListItemText
                  primary={label}
                  primaryTypographyProps={{ noWrap: true }}
                />
              </ListItemButton>

          ))}
        </List>
      </Drawer>

      {/* ─── Main Content ─── */}
      <Box
        component="main"
        sx={{
          flexGrow: 1,
          p: 3,
          mt: 25,            // отступ снизу AppBar
          //ml: `${drawerWidth}px`,
          display  : 'flex',              /* для центрирования карточки */
          justifyContent: 'center',
          alignItems: 'flex-start',
        }}
      >
        <Outlet />
      </Box>
    </Box>
  );
}
