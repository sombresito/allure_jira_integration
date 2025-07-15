/* -----------------------------------------------------------------------
 *  src/pages/ReportsFull.tsx
 *  Страница «Отчёты Full» (React + MUI X)
 * --------------------------------------------------------------------- */

import {
  AppBar,
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
  Toolbar,
  Tooltip,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import type { GridColDef, GridRowSelectionModel } from '@mui/x-data-grid';
import UploadIcon  from '@mui/icons-material/CloudUpload';
import DeleteIcon  from '@mui/icons-material/DeleteForever';
import SearchIcon  from '@mui/icons-material/Search';
import { Logotype, Button as BccButton, EButtonVariant } from 'bcc-design';
import dayjs from 'dayjs';
import utc   from 'dayjs/plugin/utc';   // ← plugin для работы с UTC
dayjs.extend(utc);                      // ← подключаем плагин
import {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';

/* ====================== DTO-типы ====================== */
interface Report {
  uuid:            string;
  createdDateTime: string;
  path:            string;
  active:          boolean;
  size:            number;
  buildUrl:        string;
}

/* ----------------- helper-fetch JSON ------------------ */
const fetchJSON = async <T,>(input: RequestInfo, init?: RequestInit) =>
  fetch(input, { credentials: 'same-origin', ...init })
    .then(r => { if (!r.ok) throw new Error(r.statusText); return r.json() as Promise<T>; });

/* ===================== Компонент ====================== */
export default function ReportsFull() {
  const theme    = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('sm'));

  /* ----------- роль пользователя (ADMIN/USER) ---------- */
  const [isAdmin, setIsAdmin] = useState(false);
  useEffect(() => {
    fetchJSON<{ roles: string[] }>('/api/user/info')
      .then(u => setIsAdmin(u.roles?.includes('ROLE_ADMIN') ?? false))
      .catch(() => setIsAdmin(false));
  }, []);

  /* ---------------- данные таблицы -------------------- */
  const [rows,    setRows]   = useState<Report[]>([]);
  const [loading, setLoad]   = useState(false);
  const [sel,     setSel]    = useState<string[]>([]);
  const [filter,  setFilter] = useState('');

  const loadRows = useCallback(() => {
    setLoad(true);
    fetchJSON<Report[]>('/api/reports')
      .then(setRows)
      .catch(() => setRows([]))
      .finally(() => setLoad(false));
  }, []);
  useEffect(() => { loadRows(); }, [loadRows]);

  /* ----------------- колонки DataGrid ----------------- */
  const columns: GridColDef[] = useMemo(() => {
    const common: GridColDef[] = [
      { field: 'createdDateTime', headerName: 'Created', width: 140,
        valueFormatter: ({ value }) =>
          dayjs.utc(value as string)   // ← трактуем строку как UTC
                       .local()                // ← переводим в местную зону
                       .format('DD-MM-YYYY HH:mm'),
      },
      { field: 'url', headerName: 'Url', flex: 1, minWidth: 200, sortable: false,
        renderCell: ({ row }) => {
          const { path, uuid } = row as Report;
          const display = `reports/${path}`;
          const href    = `/allure/reports/${uuid}/index.html`;
          return (
            <a href={href} target="_blank" rel="noopener noreferrer">
              {display}
            </a>
          );
        },
      },
    ];
    if (isAdmin) {
      return [
        { field: 'uuid',  headerName: 'ID', minWidth: 300 },
        ...common,
        { field: 'path',  headerName: 'Path', flex: 1, minWidth: 120 },
        { field: 'active', headerName: 'Active', width: 90 },
        { field: 'size',   headerName: 'Size KB', width: 100, type: 'number',
          valueGetter: ({ row }) => row.size,
        },
      ];
    }
    return common;
  }, [isAdmin]);

  /* ------------ глобальный поиск по строкам ----------- */
  const visibleRows = useMemo(() => {
    const q = filter.trim().toLowerCase();
    if (!q) return rows;
    return rows.filter(r =>
      Object.values(r).join(' ').toLowerCase().includes(q));
  }, [rows, filter]);

  /* ------------------ загрузка .zip ------------------- */
  const [upOpen, setUpOpen] = useState(false);
  const fileRef = useRef<HTMLInputElement | null>(null);

  const handleUpload = async () => {
    const file = fileRef.current?.files?.[0];
    if (!file) return;

    const form = new FormData();
    form.append('allureReportArchive', file);           // имя part == @RequestParam

    // «reportPath» = имя архива без .zip
    const reportPath = encodeURIComponent(
      file.name.replace(/\.zip$/i, ''));

    try {
      const resp = await fetch(`/api/report/${reportPath}`, {
        method:        'POST',
        body:          form,
        credentials:   'same-origin',
      });
      if (!resp.ok) throw new Error(`${resp.status} ${resp.statusText}`);
      loadRows();
      setUpOpen(false);
    } catch (e) {
      alert('Ошибка загрузки: ' + (e as Error).message);
    }
  };

  /* ------------------ удаление отчётов ---------------- */
  const handleDelete = async () => {
    if (!sel.length) return;
    try {
      await Promise.all(
        sel.map(uuid =>
          fetch('/api/reports/' + uuid, {
            method: 'DELETE',
            credentials: 'same-origin',
          }).then(r => { if (!r.ok) throw new Error(r.statusText); }),
        ),
      );
      setSel([]);
      loadRows();
    } catch (e) {
      alert('Ошибка при удалении: ' + (e as Error).message);
    }
  };

  /* ======================= UI ========================= */
  return (
    <>
      {/* -------- Fixed Header -------- */}
      <AppBar position="fixed"
        sx={{
          zIndex: t => t.zIndex.drawer + 1,
          bgcolor: '#2dba8d',
          color:   '#000',
        }}>
        <Toolbar sx={{ minHeight: 56, px: 3 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Typography component="span"
              sx={{
                fontFamily: '"Inter", sans-serif',
                fontSize:   '1.25rem',
                fontWeight: 600,
                lineHeight: 1,
                transform:  'translateY(-2px)',
              }}>
              Reports
            </Typography>
            <Box sx={{ display: 'inline-block',
                       transform: 'translateY(2px)' }}>
              <Logotype.BccHub size="sm" />
            </Box>
          </Box>
          <Box sx={{ flexGrow: 1 }} />
          <Box sx={{
            px: isMobile ? 2 : 3,
            py: isMobile ? 0.75 : 1,
            bgcolor: '#E6E7EB',
            color:  '#2E3034',
            borderRadius: 2,
            fontWeight:   600,
            '&:hover': { bgcolor: '#DADBDF' },
          }}>
            <BccButton
              view={EButtonVariant.NeutralFilledSecondary}
              size={isMobile ? 'm' : 'l'}
              onClick={() => (window.location.href = '/ui/reports-main')}
              style={{
                background: 'transparent',
                border:     0,
                padding:    0,
                minWidth:   0,
                color:      'inherit',
                font:       'inherit',
              }}>
              Вернуться&nbsp;на&nbsp;главную
            </BccButton>
          </Box>
        </Toolbar>
      </AppBar>

      {/* ---------- Main Content ---------- */}
      <Box component="main" sx={{ flexGrow: 1, p: 4, mt: '56px' }}>
        {/* --- панель поиска / кнопок --- */}
        <Stack direction={isMobile ? 'column' : 'row'}
               spacing={2} alignItems="center" mb={2}>
          <TextField size="small" placeholder="Поиск…"
                     value={filter}
                     onChange={e => setFilter(e.target.value)}
                     InputProps={{ startAdornment:
                       <SearchIcon fontSize="small" /> }} />

          <Tooltip title="Загрузить отчёт">
            <span>
              <Button variant="contained"
                      startIcon={<UploadIcon />}
                      onClick={() => setUpOpen(true)}>
                Upload
              </Button>
            </span>
          </Tooltip>

          {isAdmin && (
            <Tooltip title="Удалить выбранное">
              <span>
                <Button variant="outlined" color="error"
                        startIcon={<DeleteIcon />}
                        disabled={!sel.length}
                        onClick={handleDelete}>
                  Delete
                </Button>
              </span>
            </Tooltip>
          )}
        </Stack>

        {/* --- Таблица --- */}
        <DataGrid
          rows={visibleRows}
          columns={columns}
          getRowId={(r: Report) => r.uuid}
          checkboxSelection={isAdmin}
          disableRowSelectionOnClick
          loading={loading}
          onRowSelectionModelChange={(m: GridRowSelectionModel) =>
            setSel(m as string[])}
          sx={{ bgcolor: 'grey.100', height: 700 }}
        />

        {/* --- Диалог загрузки .zip --- */}
        <Dialog open={upOpen} onClose={() => setUpOpen(false)} fullWidth>
          <DialogTitle>Загрузка отчёта (.zip)</DialogTitle>
          <DialogContent>
            <input type="file" accept=".zip"
                   ref={ref => (fileRef.current = ref)}
                   style={{ marginTop: 16 }} />
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setUpOpen(false)}>Отмена</Button>
            <Button variant="contained" onClick={handleUpload}>
              Загрузить
            </Button>
          </DialogActions>
        </Dialog>
      </Box>
    </>
  );
}
