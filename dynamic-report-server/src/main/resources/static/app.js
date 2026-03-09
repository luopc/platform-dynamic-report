// 全局变量
const API_BASE_URL = '/api/forms';
let currentFormSchema = null;
let formFields = new Map();

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    console.log('🚀 动态表单系统启动');
    loadFormList();

    // 设置表单提交事件
    document.getElementById('dynamicForm').addEventListener('submit', function(e) {
        e.preventDefault();
        submitForm();
    });
});

/**
 * 加载表单列表
 */
async function loadFormList() {
    try {
        const response = await fetch(`${API_BASE_URL}/schemas`);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result = await response.json();

        if (result.success) {
            renderFormList(result.data);
        } else {
            showError('formList', result.message);
        }
    } catch (error) {
        console.error('加载表单列表失败:', error);
        showError('formList', '网络错误，请稍后重试');
    }
}

/**
 * 渲染表单列表
 */
function renderFormList(schemas) {
    const formListContainer = document.getElementById('formList');

    if (schemas.length === 0) {
        formListContainer.innerHTML = `
            <div class="col-span-full text-center py-8 text-gray-500">
                <i class="fas fa-inbox text-4xl mb-4"></i>
                <p>暂无可用表单</p>
            </div>
        `;
        return;
    }

    formListContainer.innerHTML = schemas.map(schema => `
        <div class="border border-gray-200 rounded-lg p-4 hover:shadow-md transition-all cursor-pointer hover:border-blue-300"
             onclick="loadForm('${schema.schemaId}')">
            <div class="flex items-start space-x-3">
                <div class="bg-blue-100 text-blue-600 p-2 rounded-lg">
                    <i class="fas fa-wpforms"></i>
                </div>
                <div class="flex-1">
                    <h3 class="font-semibold text-gray-800">${schema.name}</h3>
                    <p class="text-gray-600 text-sm mt-1">${schema.description}</p>
                    <div class="flex items-center space-x-4 mt-3">
                        <span class="text-xs bg-gray-100 text-gray-600 px-2 py-1 rounded">
                            <i class="fas fa-tag mr-1"></i>${schema.category}
                        </span>
                        <span class="text-xs text-gray-500">
                            <i class="fas fa-code-branch mr-1"></i>v${schema.version}
                        </span>
                    </div>
                </div>
            </div>
        </div>
    `).join('');
}

/**
 * 加载表单配置
 */
async function loadForm(schemaId) {
    try {
        console.log('Loading form for schema:', schemaId);

        const response = await fetch(`${API_BASE_URL}/${schemaId}/config`);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result = await response.json();
        console.log('Form config response:', result);

        if (result.success) {
            currentFormSchema = result.data;
            showForm();
            renderForm(result.data);
        } else {
            alert('加载表单失败: ' + result.message);
        }
    } catch (error) {
        console.error('加载表单失败:', error);
        alert('网络错误，请稍后重试');
    }
}

/**
 * 显示表单容器
 */
function showForm() {
    const formContainer = document.getElementById('formContainer');
    formContainer.classList.remove('hidden');
    formContainer.scrollIntoView({ behavior: 'smooth' });
}

/**
 * 渲染表单
 */
function renderForm(formConfig) {
    console.log('Rendering form with data:', formConfig);

    // 设置表单标题和描述
    document.getElementById('formTitle').textContent = formConfig.name || '表单';
    document.getElementById('formDescription').textContent = formConfig.description || '';

    // 清空之前的内容
    const dynamicForm = document.getElementById('dynamicForm');
    dynamicForm.innerHTML = '';

    // 清空字段映射
    formFields.clear();

    // 生成表单字段
    formConfig.fields.forEach(field => {
        console.log('Creating field:', field.name, field.type, field.enumValues); // 调试信息
        const fieldElement = createFormField(field);
        dynamicForm.appendChild(fieldElement);
        formFields.set(field.name, field);
    });

    console.log('Form rendered successfully');
    console.log('All fields in formFields:', Array.from(formFields.keys())); // 调试信息
}

/**
 * 创建表单字段
 */
function createFormField(field) {
    const fieldDiv = document.createElement('div');
    fieldDiv.className = 'space-y-2';

    let html = '';

    // 字段标签
    const requiredMark = field.required ? '<span class="text-red-500 ml-1">*</span>' : '';

    if (field.type !== 'boolean') {
        html += `
            <label class="block text-sm font-medium text-gray-700">
                ${field.title}${requiredMark}
            </label>
        `;
    }

    // 根据字段类型创建输入控件
    const inputHtml = createInputHtml(field);
    html += inputHtml;

    // 字段描述
    if (field.type !== 'boolean' && field.description) {
        html += `
            <p class="text-sm text-gray-500">${field.description}</p>
        `;
    }

    // 错误提示容器
    html += `
        <div id="error-${field.name}" class="text-sm text-red-600 hidden">
            <i class="fas fa-exclamation-circle mr-1"></i>
            <span class="error-message"></span>
        </div>
    `;

    fieldDiv.innerHTML = html;

    // 添加实时验证事件
    const input = fieldDiv.querySelector('input, select, textarea');
    if (input) {
        input.addEventListener('blur', () => validateField(field.name));
        input.addEventListener('input', () => clearFieldError(field.name));
    }

    return fieldDiv;
}

/**
 * 创建输入控件HTML
 */
function createInputHtml(field) {
    const attributes = {
        name: field.name,
        id: field.name,
        class: 'w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent'
    };

    if (field.required) {
        attributes.required = true;
    }

    const buildAttrs = (attrs) => {
        return Object.entries(attrs)
            .map(([key, value]) => `${key}="${value}"`)
            .join(' ');
    };

    switch (field.type) {
        case 'string':
            if (field.format === 'email') {
                return `<input type="email" ${buildAttrs(attributes)} placeholder="请输入${field.title}">`;
            } else if (field.format === 'date') {
                return `<input type="date" ${buildAttrs(attributes)}>`;
            } else if (field.pattern && field.pattern.includes('\\d')) {
                return `<input type="tel" ${buildAttrs(attributes)} placeholder="请输入${field.title}" maxlength="11">`;
            } else if (field.name.toLowerCase().includes('password')) {
                return `<input type="password" ${buildAttrs(attributes)} placeholder="请输入${field.title}">`;
            } else {
                return `<input type="text" ${buildAttrs(attributes)} placeholder="请输入${field.title}">`;
            }

        case 'integer':
        case 'number':
            return `<input type="number" ${buildAttrs(attributes)} placeholder="请输入${field.title}">`;

        case 'boolean':
            return `
                <div class="flex items-center">
                    <input type="checkbox" id="${field.name}" name="${field.name}"
                           class="w-4 h-4 text-blue-600 bg-gray-100 border-gray-300 rounded focus:ring-blue-500 focus:ring-2">
                    <label for="${field.name}" class="ml-2 text-sm font-medium text-gray-700">
                        ${field.description || field.title}
                    </label>
                </div>
            `;

        case 'array':
            if (field.enumValues && field.enumValues.length > 0) {
                return `
                    <select name="${field.name}" id="${field.name}" multiple class="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent">
                        <option value="" disabled>请选择${field.title}（可多选）</option>
                        ${field.enumValues.map(value =>
                    `<option value="${value}">${value}</option>`
                ).join('')}
                    </select>
                    <p class="text-xs text-gray-500 mt-1">按住 Ctrl/Cmd 键可多选</p>
                `;
            }
            break;

        default:
            return `<input type="text" ${buildAttrs(attributes)} placeholder="请输入${field.title}">`;
    }

    return '';
}

/**
 * 验证单个字段
 */
async function validateField(fieldName) {
    const field = formFields.get(fieldName);
    if (!field) return true;

    const inputElement = document.getElementById(fieldName);
    if (!inputElement) {
        console.warn('Input element not found for field:', fieldName);
        return true;
    }

    let value;

    if (inputElement.type === 'checkbox') {
        value = inputElement.checked;
    } else if (inputElement.type === 'number') {
        value = inputElement.value ? Number(inputElement.value) : null;
    } else if (inputElement.type === 'select-multiple') {
        value = Array.from(inputElement.selectedOptions).map(option => option.value);
    } else {
        value = inputElement.value;
    }

    // 前端基础验证
    let isValid = true;
    let errorMessage = '';

    // 必填验证
    if (field.required && (value === null || value === '' || (Array.isArray(value) && value.length === 0))) {
        isValid = false;
        errorMessage = `${field.title}不能为空`;
    }

    // 数组类型特殊验证
    if (isValid && Array.isArray(value) && field.type === 'array') {
        if (field.enumValues && value.length > 0) {
            // 验证数组中的每个值是否都在枚举范围内
            const invalidValues = value.filter(v => !field.enumValues.includes(v));
            if (invalidValues.length > 0) {
                isValid = false;
                errorMessage = `${field.title}包含了无效选项`;
            }
        }
    }

    // 长度验证
    if (isValid && typeof value === 'string') {
        if (field.minLength && value.length < field.minLength) {
            isValid = false;
            errorMessage = `${field.title}至少需要${field.minLength}个字符`;
        }
        if (field.maxLength && value.length > field.maxLength) {
            isValid = false;
            errorMessage = `${field.title}不能超过${field.maxLength}个字符`;
        }
    }

    // 数值范围验证
    if (isValid && typeof value === 'number') {
        if (field.minimum !== null && value < field.minimum) {
            isValid = false;
            errorMessage = `${field.title}不能小于${field.minimum}`;
        }
        if (field.maximum !== null && value > field.maximum) {
            isValid = false;
            errorMessage = `${field.title}不能大于${field.maximum}`;
        }
    }

    // 正则表达式验证 - 只对非空值进行验证
    if (isValid && field.pattern && typeof value === 'string' && value.trim() !== '') {
        try {
            const regex = new RegExp(field.pattern);
            if (!regex.test(value)) {
                isValid = false;
                errorMessage = `${field.title}格式不正确`;
            }
        } catch (e) {
            console.warn('正则表达式无效:', field.pattern);
        }
    }

    // 后端验证 - 只对非空值或必填字段进行验证
    if (isValid && currentFormSchema && (field.required || (value !== null && value !== '' && !(Array.isArray(value) && value.length === 0)))) {
        try {
            const response = await fetch(`${API_BASE_URL}/${currentFormSchema.schemaId}/validate-field`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    fieldName: fieldName,
                    fieldValue: value
                })
            });

            if (response.ok) {
                const result = await response.json();
                if (result.success && !result.data.valid) {
                    isValid = false;
                    errorMessage = result.data.errors[0]?.message || `${field.title}验证失败`;
                }
            }
        } catch (error) {
            console.warn('后端验证失败:', error);
        }
    }

    // 显示或清除错误
    if (!isValid) {
        showFieldError(fieldName, errorMessage);
    } else {
        clearFieldError(fieldName);
    }

    return isValid;
}

/**
 * 显示字段错误
 */
function showFieldError(fieldName, message) {
    const errorElement = document.getElementById(`error-${fieldName}`);
    const inputElement = document.getElementById(fieldName);

    if (errorElement) {
        errorElement.querySelector('.error-message').textContent = message;
        errorElement.classList.remove('hidden');
    }

    if (inputElement) {
        inputElement.classList.add('border-red-500');
    }
}

/**
 * 清除字段错误
 */
function clearFieldError(fieldName) {
    const errorElement = document.getElementById(`error-${fieldName}`);
    const inputElement = document.getElementById(fieldName);

    if (errorElement) {
        errorElement.classList.add('hidden');
    }

    if (inputElement) {
        inputElement.classList.remove('border-red-500');
    }
}

/**
 * 提交表单
 */
async function submitForm(event) {
    if (event) {
        event.preventDefault();
    }

    if (!currentFormSchema) return;

    // 验证所有字段
    let isValid = true;
    for (const fieldName of formFields.keys()) {
        const fieldValid = await validateField(fieldName);
        if (!fieldValid) {
            isValid = false;
        }
    }

    if (!isValid) {
        alert('请修正表单中的错误后再提交');
        return;
    }

    // 收集表单数据
    const formData = collectFormData();

    // 禁用提交按钮
    const submitButton = document.querySelector('button[type="submit"]');
    const originalText = submitButton.innerHTML;
    submitButton.disabled = true;
    submitButton.innerHTML = '<div class="loading-spinner inline-block mr-2"></div>提交中...';

    try {
        const response = await fetch(`${API_BASE_URL}/${currentFormSchema.schemaId}/submit`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(formData)
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result = await response.json();

        if (result.success) {
            showSuccessMessage('表单提交成功！提交ID: ' + result.data.submissionId);
            resetForm();
        } else {
            alert('提交失败: ' + result.message);
        }
    } catch (error) {
        console.error('提交表单失败:', error);
        alert('网络错误，请稍后重试');
    } finally {
        // 恢复提交按钮
        submitButton.disabled = false;
        submitButton.innerHTML = originalText;
    }
}

/**
 * 收集表单数据
 */
function collectFormData() {
    const data = {};

    formFields.forEach((field, fieldName) => {
        const inputElement = document.getElementById(fieldName);

        console.log(`Processing field: ${fieldName}, element found:`, !!inputElement); // 调试信息
        if (!inputElement) {
            console.warn(`Input element not found for field: ${fieldName}`); // 调试信息
            return;
        }

        let value;
        if (inputElement.type === 'checkbox') {
            value = inputElement.checked;
        } else if (inputElement.type === 'number') {
            value = inputElement.value ? Number(inputElement.value) : null;
        } else if (inputElement.type === 'select-multiple') {
            value = Array.from(inputElement.selectedOptions).map(option => option.value);
        } else {
            value = inputElement.value ? inputElement.value.trim() : null;
        }

        // 空值处理 - 对于可选字段，如果值为空字符串则设为null
        // 但数组类型字段除外，空数组应该保留
        if (value === '' && !Array.isArray(value)) {
            value = null;
        }

        // 处理嵌套对象
        if (fieldName.includes('.')) {
            const parts = fieldName.split('.');
            let current = data;

            // 创建嵌套对象结构
            for (let i = 0; i < parts.length - 1; i++) {
                if (!current[parts[i]]) {
                    current[parts[i]] = {};
                }
                current = current[parts[i]];
            }

            current[parts[parts.length - 1]] = value;
        } else {
            data[fieldName] = value;
        }

        console.log(`Field ${fieldName}:`, value, 'Type:', typeof value, 'Is Array:', Array.isArray(value)); // 调试输出
    });

    console.log('Collected form data:', data); // 调试输出
    return data;
}

/**
 * 重置表单
 */
function resetForm() {
    const form = document.getElementById('dynamicForm');
    if (form) {
        form.reset();
    }

    // 清除所有错误提示
    formFields.forEach((field, fieldName) => {
        clearFieldError(fieldName);
    });

    hideSuccessMessage();
}

/**
 * 关闭表单
 */
function closeForm() {
    const formContainer = document.getElementById('formContainer');
    formContainer.classList.add('hidden');
    resetForm();
    hideSuccessMessage();
    currentFormSchema = null;
}

/**
 * 显示成功消息
 */
function showSuccessMessage(message) {
    const successElement = document.getElementById('successMessage');
    const successText = document.getElementById('successText');

    successText.textContent = message;
    successElement.classList.remove('hidden');

    // 3秒后自动隐藏
    setTimeout(() => {
        hideSuccessMessage();
    }, 3000);
}

/**
 * 隐藏成功消息
 */
function hideSuccessMessage() {
    document.getElementById('successMessage').classList.add('hidden');
}

/**
 * 显示统计信息
 */
async function showStatistics() {
    try {
        const response = await fetch(`${API_BASE_URL}/statistics`);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const result = await response.json();

        if (result.success) {
            renderStatistics(result.data);
            document.getElementById('statisticsModal').classList.remove('hidden');
        } else {
            alert('获取统计信息失败: ' + result.message);
        }
    } catch (error) {
        console.error('获取统计信息失败:', error);
        alert('网络错误，请稍后重试');
    }
}

/**
 * 渲染统计信息
 */
function renderStatistics(stats) {
    const content = document.getElementById('statisticsContent');

    content.innerHTML = `
        <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div class="bg-blue-50 rounded-lg p-6">
                <div class="flex items-center">
                    <div class="bg-blue-100 text-blue-600 p-3 rounded-lg mr-4">
                        <i class="fas fa-wpforms text-xl"></i>
                    </div>
                    <div>
                        <p class="text-sm text-blue-600">总提交数</p>
                        <p class="text-2xl font-bold text-blue-800">${stats.totalSubmissions}</p>
                    </div>
                </div>
            </div>

            <div class="bg-green-50 rounded-lg p-6">
                <div class="flex items-center">
                    <div class="bg-green-100 text-green-600 p-3 rounded-lg mr-4">
                        <i class="fas fa-list-alt text-xl"></i>
                    </div>
                    <div>
                        <p class="text-sm text-green-600">可用表单</p>
                        <p class="text-2xl font-bold text-green-800">${stats.totalSchemas}</p>
                    </div>
                </div>
            </div>
        </div>

        <div class="mt-6">
            <h4 class="font-semibold text-gray-800 mb-4">提交状态分布</h4>
            <div class="space-y-3">
                ${Object.entries(stats.statusCount).map(([status, count]) => `
                    <div class="flex items-center justify-between bg-gray-50 rounded-lg p-3">
                        <span class="text-gray-700 capitalize">${getStatusText(status)}</span>
                        <span class="bg-gray-200 text-gray-800 px-3 py-1 rounded-full text-sm font-medium">
                            ${count}
                        </span>
                    </div>
                `).join('')}
            </div>
        </div>
    `;
}

/**
 * 获取状态文本
 */
function getStatusText(status) {
    const statusMap = {
        'pending': '待处理',
        'approved': '已通过',
        'rejected': '已拒绝'
    };
    return statusMap[status] || status;
}

/**
 * 关闭统计信息
 */
function closeStatistics() {
    document.getElementById('statisticsModal').classList.add('hidden');
}

/**
 * 显示错误信息
 */
function showError(containerId, message) {
    const container = document.getElementById(containerId);
    container.innerHTML = `
        <div class="text-center py-8 text-red-500">
            <i class="fas fa-exclamation-triangle text-4xl mb-4"></i>
            <p>${message}</p>
            <button onclick="loadFormList()" class="mt-4 px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700">
                重试
            </button>
        </div>
    `;
}
